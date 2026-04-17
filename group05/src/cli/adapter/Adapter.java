// cli/adapter/Adapter.java
package cli.adapter;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.results.*;
import base.storage.GamePersistenceService;
import cli.TerminalParser;
import cli.elements.Response;
import cli.events.MessageIOEvent;

import static cli.events.BidEvents.*;
import static cli.events.CountEvents.*;
import static cli.events.PlayEvents.*;

import java.util.List;

/**
 * Translates between UI input/output and domain commands/results.
 * <p>
 * Owns persistence orchestration (Pure Fabrication / Controller): when a state
 * emits {@link SaveDescriptionResult} and the user supplies a description, the
 * adapter performs the save itself and then hands back a neutral command so the
 * state can resume its own flow without any IO knowledge.
 */
public class Adapter {

    private final TerminalParser parser;
    private final WhistGame game;
    private final GamePersistenceService persistenceService;

    public Adapter(WhistGame game) {
        this(game, new GamePersistenceService());
    }

    public Adapter(WhistGame game, GamePersistenceService persistenceService) {
        if (game == null) throw new IllegalArgumentException("game cannot be null");
        if (persistenceService == null) throw new IllegalArgumentException("persistenceService cannot be null");
        this.parser = new TerminalParser();
        this.game = game;
        this.persistenceService = persistenceService;
    }

    public AdapterResult handleResult(GameResult result) {
        return switch (result) {

            case PlayCardResult p -> {
                Player player = p.player();
                if (!player.getRequiresConfirmation()) {
                    Card chosen = player.chooseCard(
                            p.tableCards().isEmpty() ? null : p.tableCards().getFirst().suit());
                    yield new AdapterResult.Immediate(new CardCommand(chosen));
                }
                yield new AdapterResult.NeedsIO(
                        List.of(new ConfirmationIOEvent(player.getName())),
                        new PlayCardIOEvent(p));
            }

            case BidTurnResult b -> {
                Player player = b.player();
                if (!player.getRequiresConfirmation()) {
                    Bid botBid = player.chooseBid();
                    Suit chosenTrump = botBid.determineTrump(b.trumpSuit());
                    yield new AdapterResult.Immediate(new BidCommand(botBid.getType(), chosenTrump));
                }
                yield new AdapterResult.NeedsIO(List.of(), new BidTurnIOEvent(b));
            }

            case SuitSelectionRequired ignored ->
                    new AdapterResult.NeedsIO(List.of(), new SuitSelectionIOEvent());

            case ProposalRejected p ->
                    new AdapterResult.NeedsIO(List.of(), new ProposalRejectedIOEvent(p));

            case BiddingCompleted ignored ->
                    new AdapterResult.NeedsIO(List.of(), new BiddingCompletedIOEvent());

            case BidSelectionResult b ->
                    new AdapterResult.NeedsIO(List.of(), new BidSelectionIOEvent(b.availableBids()));

            case SuitSelectionResult ignored ->
                    new AdapterResult.NeedsIO(List.of(), new SuitSelectionIOEvent());

            case PlayerSelectionResult p ->
                    new AdapterResult.NeedsIO(List.of(),
                            new PlayerSelectionIOEvent(p.players(), p.multiSelect()));

            case AmountOfTrickWonResult ignored ->
                    new AdapterResult.NeedsIO(List.of(), new TrickInputIOEvent());

            case SaveDescriptionResult ignored ->
                    new AdapterResult.NeedsIO(List.of(), new SaveDescriptionIOEvent());

            case ScoreBoardResult s ->
                    new AdapterResult.NeedsIO(List.of(), new ScoreBoardIOEvent(s.names(), s.scores()));

            case EndOfTurnResult e ->
                    new AdapterResult.NeedsIO(List.of(), new EndOfTurnIOEvent(e));

            case EndOfTrickResult e ->
                    new AdapterResult.NeedsIO(List.of(), new EndOfTrickIOEvent(e));

            case EndOfRoundResult e ->
                    new AdapterResult.NeedsIO(List.of(), new EndOfRoundIOEvent(e));

            case TrickHistoryResult t ->
                    new AdapterResult.NeedsIO(List.of(), new TrickHistoryIOEvent(t));

            case ParticipatingPlayersResult p ->
                    new AdapterResult.NeedsIO(List.of(), new ParticipatingPlayersIOEvent(p));

            default -> throw new IllegalStateException("Unexpected GameResult: " + result);
        };
    }

    public AdapterResponse handleResponse(Response response, GameResult result) {
        if (response.rawInput() == null || response.rawInput().isBlank()) {
            return AdapterResponse.toDomain(null);
        }
        String raw = response.rawInput().trim();

        try {
            return switch (result) {
                case BidTurnResult b -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new BidCommand(b.availableBids().get(choice - 1)));
                }
                case SuitSelectionRequired ignored -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new SuitCommand(Suit.values()[choice - 1]));
                }
                case ProposalRejected ignored -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(
                            new BidCommand(choice == 1 ? BidType.PASS : BidType.SOLO_PROPOSAL));
                }
                case BiddingCompleted ignored -> AdapterResponse.toDomain(null);

                case BidSelectionResult b -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new BidCommand(b.availableBids()[choice - 1]));
                }
                case SuitSelectionResult ignored -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new SuitCommand(Suit.values()[choice - 1]));
                }
                case PlayerSelectionResult ignored -> {
                    if (raw.equals("0")) yield AdapterResponse.toDomain(new PlayerListCommand(List.of()));
                    List<Integer> indices = parser.parseNumbersInput(raw);
                    List<Player> players = indices.stream()
                            .map(i -> game.getPlayers().get(i - 1))
                            .toList();
                    yield AdapterResponse.toDomain(new PlayerListCommand(players));
                }
                case AmountOfTrickWonResult ignored -> {
                    int tricks = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new NumberCommand(tricks));
                }
                case ScoreBoardResult ignored -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new NumberCommand(choice));
                }

                // The save is the adapter's responsibility. The state only gets a
                // TextCommand afterwards to signal "done, resume your flow".
                case SaveDescriptionResult s -> {
                    String description = parser.parseString(raw);
                    try {
                        persistenceService.save(game, s.mode(), description);
                    } catch (RuntimeException e) {
                        yield AdapterResponse.uiOnly(
                                new MessageIOEvent("Save failed: " + e.getMessage()));
                    }
                    yield AdapterResponse.toDomain(new TextCommand(description));
                }

                case EndOfTurnResult _, EndOfTrickResult _, EndOfRoundResult _, TrickHistoryResult _ ->
                        AdapterResponse.toDomain(null);

                case ParticipatingPlayersResult p -> {
                    List<Integer> indices = parser.parseNumbersInput(raw);
                    List<Player> players = indices.stream()
                            .map(i -> p.playerNames().get(i - 1))
                            .map(name -> game.getPlayers().stream()
                                    .filter(player -> player.getName().equals(name))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException("Player not found: " + name)))
                            .toList();
                    yield AdapterResponse.toDomain(new PlayerListCommand(players));
                }

                case PlayCardResult p -> {
                    Player player = p.player();
                    if (!player.getRequiresConfirmation()) {
                        Card chosen = player.chooseCard(
                                p.tableCards().isEmpty() ? null : p.tableCards().getFirst().suit());
                        yield AdapterResponse.toDomain(new CardCommand(chosen));
                    }
                    int choice = parser.parseNumberInput(raw);
                    if (choice == 0) {
                        if (p.lastPlayedTrick() == null) {
                            yield AdapterResponse.uiOnly(
                                    new MessageIOEvent("No tricks have been played yet!"));
                        }
                        yield AdapterResponse.uiOnly(
                                new TrickHistoryIOEvent(new TrickHistoryResult(p.lastPlayedTrick())));
                    }
                    if (choice < 1 || choice > p.legalCards().size()) {
                        yield AdapterResponse.uiOnly(
                                new MessageIOEvent("Invalid selection. Choose 0 to " + p.legalCards().size()));
                    }
                    Card selected = p.legalCards().get(choice - 1);
                    yield AdapterResponse.toDomain(new CardCommand(selected));
                }

                default -> throw new IllegalStateException(
                        "Unexpected GameResult in response handling: " + result);
            };
        } catch (Exception e) {
            return AdapterResponse.uiOnly(
                    new MessageIOEvent("Invalid input: \"" + raw + "\". Please try again."));
        }
    }
}