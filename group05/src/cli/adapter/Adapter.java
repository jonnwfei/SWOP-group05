package cli.adapter;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import base.domain.results.*;
import base.domain.round.Round;
import cli.TerminalParser;
import cli.elements.Response;

import cli.events.*;
import cli.events.CountEvents.*;
import cli.events.BidEvents.*;
import cli.events.PlayEvents.*;
import cli.events.menu.AddHumanPlayerIOEvent;
import cli.events.menu.AddPlayerIOEvent;
import cli.events.menu.DeleteRoundIOEvent;
import cli.events.MessageIOEvent;

import static cli.events.BidEvents.*;
import static cli.events.CountEvents.*;
import static cli.events.PlayEvents.*;

import java.util.List;

public class Adapter {

    private final TerminalParser parser;
    private final WhistGame game;

    public Adapter(WhistGame game) {
        this.parser = new TerminalParser();
        this.game = game;
    }

    public AdapterResult handleResult(GameResult result) {
        return switch (result) {

            // =========================
            // PLAY CARD
            // =========================
            case PlayCardResult p -> {
                Player player = p.player();

                // BOT → immediate domain command
                if (!player.getRequiresConfirmation()) {
                    Card chosen = player.chooseCard(
                            p.tableCards().isEmpty() ? null : p.tableCards().getFirst().suit());
                    yield new AdapterResult.Immediate(new CardCommand(chosen));
                }

                // HUMAN → show UI (+ optional confirmation preamble)
                yield new AdapterResult.NeedsIO(
                        List.of(new ConfirmationIOEvent(player.getName())),
                        new PlayCardIOEvent(p));
            }

            // =========================
            // BIDDING
            // =========================
            case BidTurnResult b -> {
                Player player = b.player();

                if (!player.getRequiresConfirmation()) {
                    Bid botBid = player.chooseBid();
                    // BidCommand with suit so domain skips SuitSelectionRequired
                    Suit chosenTrump = botBid.determineTrump(b.trumpSuit());
                    yield new AdapterResult.Immediate(new BidCommand(botBid.getType(), chosenTrump));
                }

                yield new AdapterResult.NeedsIO(
                        List.of(),
                        new BidTurnIOEvent(b));
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

            // =========================
            // PLAYER SELECTION / COUNT
            // =========================
            case PlayerSelectionResult p ->
                new AdapterResult.NeedsIO(
                        List.of(),
                        new PlayerSelectionIOEvent(p.players(), p.multiSelect()));

            case AmountOfTrickWonResult ignored ->
                new AdapterResult.NeedsIO(List.of(), new TrickInputIOEvent());

            case SaveDescriptionResult ignored ->
                new AdapterResult.NeedsIO(List.of(), new SaveDescriptionIOEvent());

            case ScoreBoardResult s ->
                new AdapterResult.NeedsIO(List.of(), new ScoreBoardIOEvent(s.names(), s.scores()));

            // =========================
            // FLOW EVENTS (ENTER TO CONTINUE)
            // =========================
            case EndOfTurnResult e ->
                new AdapterResult.NeedsIO(List.of(), new EndOfTurnIOEvent(e));

            case EndOfTrickResult e ->
                new AdapterResult.NeedsIO(List.of(), new EndOfTrickIOEvent(e));

            case EndOfRoundResult e ->
                new AdapterResult.NeedsIO(List.of(), new EndOfRoundIOEvent(e));

            case TrickHistoryResult t ->
                new AdapterResult.NeedsIO(List.of(), new TrickHistoryIOEvent(t));

            case ParticipatingPlayersResult p ->
                new AdapterResult.NeedsIO(
                        List.of(),
                        new ParticipatingPlayersIOEvent(p));

            case AddHumanPlayerResult ignored ->
                    new AdapterResult.NeedsIO(List.of(), new AddHumanPlayerIOEvent());

            case AddPlayerResult ignored ->
                    new AdapterResult.NeedsIO(List.of(), new AddPlayerIOEvent());

            case DeleteRoundResult g ->
                    new AdapterResult.NeedsIO(List.of(), new DeleteRoundIOEvent(g.rounds()));
            // =========================
            // SAFETY
            // =========================
            default -> throw new IllegalStateException(
                    "Unexpected GameResult: " + result);
        };
    }

    public AdapterResponse handleResponse(Response response, GameResult result) {
        if (response.rawInput() == null || response.rawInput().isBlank()) {
            return AdapterResponse.toDomain(null);
        }

        String raw = response.rawInput().trim();

        try {
            return switch (result) {
                case DeleteRoundResult dr -> {
                    int choice = parser.parseNumberInput(raw);
                    if (choice == 0) {
                        // Return a Command that signals a cancel or just a Continue
                        yield AdapterResponse.toDomain(new NumberCommand(0));
                    }
                    // Get the actual round from the list provided in the result
                    Round selectedRound = dr.rounds().get(choice - 1);
                    yield AdapterResponse.toDomain(new RoundCommand(selectedRound));
                }
                case AddPlayerResult ignored -> {
                    int choice  = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new NumberCommand(choice));
                }
                case AddHumanPlayerResult e -> AdapterResponse.toDomain(new PlayerListCommand(List.of(new Player(new HumanStrategy(), raw))));
                // --- Bidding State ---
                case BidTurnResult b -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(
                            new BidCommand(b.availableBids().get(choice - 1)));
                }
                case SuitSelectionRequired ignored -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new SuitCommand(Suit.values()[choice - 1]));
                }

                case ProposalRejected ignored -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new BidCommand(choice == 1 ? BidType.PASS : BidType.SOLO_PROPOSAL));
                }

                case BiddingCompleted ignored -> AdapterResponse.toDomain(null);

                // --- Count/Setup State ---
                case BidSelectionResult b -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new BidCommand(b.availableBids()[choice - 1]));
                }

                case SuitSelectionResult ignored -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new SuitCommand(Suit.values()[choice - 1]));
                }

                case PlayerSelectionResult ignored -> {
                    if (raw.equals("0")) {
                        yield AdapterResponse.toDomain(new PlayerListCommand(List.of()));
                    }
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

                case SaveDescriptionResult ignored -> {
                    String text = parser.parseString(raw);
                    yield AdapterResponse.toDomain(new TextCommand(text));
                }

                // --- Gameplay Results (Usually just "Press Enter") ---
                case EndOfTurnResult _,EndOfTrickResult _,EndOfRoundResult _,TrickHistoryResult _ ->
                    AdapterResponse.toDomain(null);

                case ParticipatingPlayersResult p -> {
                    List<Integer> indices = parser.parseNumbersInput(raw);

                    List<Player> players = indices.stream()
                            // 1. Map the user's 1-based input to the name they actually saw on screen
                            .map(i -> p.playerNames().get(i - 1))
                            // 2. Map that name to the actual Player object in the game
                            .map(name -> game.getPlayers().stream()
                                    .filter(player -> player.getName().equals(name))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException("Player not found: " + name)))
                            .toList();

                    yield AdapterResponse.toDomain(new PlayerListCommand(players));
                }

                // --- Play Card
                case PlayCardResult p -> {
                    Player player = p.player();
                    // BOT: auto-play
                    if (!player.getRequiresConfirmation()) {
                        Card chosen = player
                                .chooseCard(p.tableCards().isEmpty() ? null : p.tableCards().getFirst().suit());

                        yield AdapterResponse.toDomain(new CardCommand(chosen));
                    }

                    // HUMAN: parse input
                    int choice = parser.parseNumberInput(raw);

                    if (choice == 0) {
                        if (p.lastPlayedTrick() == null) {
                            yield AdapterResponse.uiOnly(new MessageIOEvent("No tricks have been played yet!"));
                        } else {
                            yield AdapterResponse.uiOnly(
                                    new TrickHistoryIOEvent(new TrickHistoryResult(p.lastPlayedTrick())));
                        }
                    }

                    if (choice < 1 || choice > p.legalCards().size()) {
                        yield AdapterResponse.uiOnly(
                                new MessageIOEvent("Invalid selection. Choose 0 to " + p.legalCards().size()));
                    }

                    Card selected = p.legalCards().get(choice - 1);
                    yield AdapterResponse.toDomain(new CardCommand(selected));
                }

                default -> throw new IllegalStateException("Unexpected GameResult in response handling: " + result);
            };
        } catch (Exception e) {
            return AdapterResponse.uiOnly(new MessageIOEvent("Invalid input: \"" + raw + "\". Please try again."));
        }
    }
}