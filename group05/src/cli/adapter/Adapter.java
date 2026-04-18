package cli.adapter;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.commands.*;
import base.domain.results.*;
import base.domain.round.Round;
import base.domain.strategy.HumanStrategy;
import cli.TerminalParser;
import cli.elements.Response;

import cli.events.MessageIOEvent;

import static cli.events.BidEvents.*;
import static cli.events.CountEvents.*;
import static cli.events.MenuEvents.*;
import static cli.events.PlayEvents.*;

import java.util.List;

public class Adapter {

    private final TerminalParser parser;
    private final WhistGame game;

    /**
     * Initializes the Adapter with a reference to the WhistGame.
     * @param game The current gameInstance, needed to map user input to actual Player objects and access game state for context when parsing responses.
     */
    public Adapter(WhistGame game) {
        this.parser = new TerminalParser();
        this.game = game;
    }

    /**
     * Converts a GameResult from the domain into an AdapterResult, which indicates either an immediate domain
     * command to execute (for bot actions) or a UI event that requires user input (for human actions).
     * @param result The GameResult coming from the domain after executing a state step, determines how the adapter should respond.
     * @return An AdapterResult indicating either an immediate domain command or a UI event to be rendered for user input.
     */
    public AdapterResult handleResult(GameResult result) {
        return switch (result) {

            // =========================
            // PLAY CARD
            // =========================
            case PlayCardResult p -> {
                Player player = p.player();

                // BOT → immediate domain command
                if (!(player.getDecisionStrategy() instanceof HumanStrategy)) {
                    Card chosen = player.chooseCard(
                            p.turns().isEmpty() ? null : p.turns().getFirst().playedCard().suit());
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

                if (!(player.getDecisionStrategy() instanceof HumanStrategy)) {
                    Bid botBid = player.chooseBid();
                    Suit dealtTrump = b.trumpSuit();

                    // In no-trump rounds, avoid asking bids that mirror dealt trump to resolve from null.
                    // For suit-requiring bids, pass a non-null placeholder so the bid can return its own chosen suit.
                    Suit chosenTrump = null;
                    if (botBid.getType().getRequiresSuit()) {
                        Suit safeDealtTrump = dealtTrump != null ? dealtTrump : Suit.CLUBS;
                        chosenTrump = botBid.determineTrump(safeDealtTrump);
                    } else if (dealtTrump != null) {
                        chosenTrump = botBid.determineTrump(dealtTrump);
                    }

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
                        new PlayerSelectionIOEvent(p.players(), p.multiSelect(), p.type()));

            case AmountOfTrickWonResult ignored ->
                new AdapterResult.NeedsIO(List.of(), new TrickInputIOEvent());

            case SaveDescriptionResult ignored ->
                new AdapterResult.NeedsIO(List.of(), new SaveDescriptionIOEvent());

            case ScoreBoardResult s ->
                new AdapterResult.NeedsIO(List.of(), new ScoreBoardIOEvent(s.names(), s.scores(), s.canRemovePlayer()));

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
        };
    }

    /**
     * Transforms the raw user input into a domain command based on the current GameResult context.
     *
     * @param response The raw user input from the terminal.
     * @param result The current GameResult that the user is responding to, which determines how the input should be parsed.
     * @return AdapterResponse containing either a domain command to be executed or a UI-only event (e.g. error message, if the input was invalid.)
     */
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
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new NumberCommand(choice));
                }
                case AddHumanPlayerResult ignored -> {
                    String name = parser.parseString(raw);
                    yield AdapterResponse.toDomain(new TextCommand(name));
                }
                // --- Bidding State ---
                case BidTurnResult b -> {
                    int choice = parser.parseNumberInput(raw);

                    if (choice < 1 || choice > b.availableBids().size()) {
                        throw new IllegalArgumentException("Invalid bid selection");
                    }

                    yield AdapterResponse.toDomain(
                            new BidCommand(b.availableBids().get(choice - 1)));
                }
                case SuitSelectionRequired ignored -> {
                    int choice = parser.parseNumberInput(raw);

                    if (choice < 1 || choice > Suit.values().length) {
                        throw new IllegalArgumentException("Invalid suit selection");
                    }

                    yield AdapterResponse.toDomain(new SuitCommand(Suit.values()[choice - 1]));
                }

                case ProposalRejected ignored -> {
                    int choice = parser.parseNumberInput(raw);

                    if (choice != 1 && choice != 2) {
                        throw new IllegalArgumentException("Invalid choice");
                    }

                    yield AdapterResponse.toDomain(
                            new BidCommand(choice == 1 ? BidType.PASS : BidType.SOLO_PROPOSAL));
                }

                case BiddingCompleted ignored -> AdapterResponse.toDomain(null);

                // --- Count/Setup State ---
                case BidSelectionResult b -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new BidCommand(b.availableBids()[choice - 1]));
                }

                case SuitSelectionResult ignored -> {
                    int choice = parser.parseNumberInput(raw);

                    if (choice < 1 || choice > Suit.values().length) {
                        throw new IllegalArgumentException("Invalid suit selection");
                    }

                    yield AdapterResponse.toDomain(new SuitCommand(Suit.values()[choice - 1]));
                }

                case PlayerSelectionResult p -> {
                    if (raw.equals("0")) {
                        if (requiresAtLeastOne(p.type())) {
                            throw new IllegalArgumentException("At least one player required");
                        }
                        yield AdapterResponse.toDomain(new PlayerListCommand(List.of()));
                    }

                    List<Integer> indices = parser.parseNumbersInput(raw);

                    List<PlayerId> players = indices.stream()
                            .map(i -> game.getPlayers().get(i - 1).getId())
                            .toList();

                    validatePlayerSelection(p.type(), players.size());

                    yield AdapterResponse.toDomain(new PlayerListCommand(players));
                }

                case AmountOfTrickWonResult ignored -> {
                    int tricks = parser.parseNumberInput(raw);

                    if (tricks < 0 || tricks > 13) {
                        throw new IllegalArgumentException("Tricks cannot be negative");
                    }
                    yield AdapterResponse.toDomain(new NumberCommand(tricks));
                }

                case ScoreBoardResult ignored -> {
                    int choice = parser.parseNumberInput(raw);

                    // TODO: define valid range (depends on UI options)
                    if (choice < 1) {
                        throw new IllegalArgumentException("Invalid scoreboard selection");
                    }

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

                    int max = p.playerNames().size();
                    for (int i : indices) {
                        if (i < 1 || i > max) {
                            throw new IllegalArgumentException("Player index out of range");
                        }
                    }

                    List<PlayerId> playerIds = indices.stream()
                            .map(i -> p.playerNames().get(i - 1))
                            .map(name -> game.getPlayers().stream()
                                    .filter(player -> player.getName().equals(name))
                                    .findFirst()
                                    .map(Player::getId)
                                    .orElseThrow(() -> new IllegalArgumentException("Player not found: " + name)))
                            .toList();

                    yield AdapterResponse.toDomain(new PlayerListCommand(playerIds));
                }

                // --- Play Card
                case PlayCardResult p -> {
                    Player player = p.player();

                    if (!(player.getDecisionStrategy() instanceof HumanStrategy)) {
                        Card chosen = player
                                .chooseCard(p.turns().isEmpty() ? null : p.turns().getFirst().playedCard().suit());

                        yield AdapterResponse.toDomain(new CardCommand(chosen));
                    }

                    int choice = parser.parseNumberInput(raw);

                    if (choice == 0) {
                        if (p.lastPlayedTrick() == null) {
                            yield AdapterResponse.uiOnly(new MessageIOEvent("No tricks have been played yet!"));
                        } else {
                            yield AdapterResponse.uiOnly(
                                    new TrickHistoryIOEvent(new TrickHistoryResult(p.lastPlayedTrick())));
                        }
                    }

                    int max = p.legalCards().size();

                    if (choice < 1 || choice > max) {
                        throw new IllegalArgumentException("Invalid card selection");
                    }

                    Card selected = p.legalCards().get(choice - 1);
                    yield AdapterResponse.toDomain(new CardCommand(selected));
                }

            };
        } catch (Exception e) {
            return AdapterResponse.uiOnly(new MessageIOEvent("Invalid input: \"" + raw + "\". Please try again."));
        }
    }
    private void validatePlayerSelection(BidType type, int count) {
        switch (type) {
            case MISERIE, OPEN_MISERIE -> {
                if (count < 1) {
                    throw new IllegalArgumentException("At least one player required");
                }
            }
            case PROPOSAL, TROEL, TROELA -> {
                if (count != 2) {
                    throw new IllegalArgumentException("Exactly two players required");
                }
            }
            default -> {
                if (count != 1) {
                    throw new IllegalArgumentException("Exactly one player required");
                }
            }
        }
    }

    private boolean requiresAtLeastOne(BidType type) {
        return switch (type) {
            case MISERIE, OPEN_MISERIE -> false;
            case PROPOSAL, TROEL, TROELA -> true;
            default -> true;
        };
    }
}