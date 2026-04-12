package cli.Adapter;

import base.domain.WhistGame;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.results.*;
import cli.TerminalParser;
import cli.elements.Response;
import cli.events.*;
import cli.events.CountEvents.*;
import cli.events.BidEvents.*;
import cli.events.PlayEvents.*;

import java.util.List;

public class Adapter {

    private final TerminalParser parser;
    private final WhistGame game;

    public Adapter(WhistGame game) {
        this.parser = new TerminalParser();
        this.game = game;
    }

    public IOEvent handleResult(GameResult result) {
        return switch (result) {
            case BidTurnResult b               -> new BidTurnIOEvent(b);
            case SuitSelectionRequired ignored -> new SuitSelectionIOEvent();
            case ProposalRejected p            -> new ProposalRejectedIOEvent(p);
            case BiddingCompleted ignored      -> new BiddingCompletedIOEvent();
            case BidSelectionResult b          -> new BidSelectionIOEvent(b.availableBids());
            case SuitSelectionResult s         -> new SuitSelectionIOEvent();
            case PlayerSelectionResult p       -> new PlayerSelectionIOEvent(p.players(), p.multiSelect());
            case TrickInputResult ignored      -> new TrickInputIOEvent();
            case ScoreBoardResult s            -> new ScoreBoardIOEvent(s.names(), s.scores());
            case SaveDescriptionResult ignored -> new SaveDescriptionIOEvent();
            case ScoreBoardCompleteResult ignored -> new ScoreBoardCompleteIOEvent();
            case PlayCardResult p              -> new PlayCardIOEvent(p);
            case EndOfTurnResult e             -> new EndOfTurnIOEvent(e);
            case EndOfTrickResult e            -> new EndOfTrickIOEvent(e);
            case EndOfRoundResult e            -> new EndOfRoundIOEvent(e);
            case TrickHistoryResult t          -> new TrickHistoryIOEvent(t);
            case ParticipatingPlayersResult p  -> new ParticipatingPlayersIOEvent(p);
            default -> throw new IllegalStateException("Unexpected GameResult: " + result);
        };
    }

    public AdapterResponse handleResponse(Response response, GameResult result) {
        if (response.rawInput() == null) {
            return AdapterResponse.toDomain(new ContinueCommand());
        }

        String raw = response.rawInput().trim();

        try {
            return switch (result) {
                // --- Bidding State ---
                case BidTurnResult b -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new BidCommand(b.availableBids()[choice - 1]));
                }

                case SuitSelectionRequired ignored -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new SuitCommand(Suit.values()[choice - 1]));
                }

                case ProposalRejected ignored -> {
                    int choice = parser.parseNumberInput(raw);
                    yield AdapterResponse.toDomain(new BidCommand(choice == 1 ? BidType.PASS : BidType.SOLO_PROPOSAL));
                }

                case BiddingCompleted ignored -> AdapterResponse.toDomain(new ContinueCommand());

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

                case TrickInputResult ignored -> {
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
                case EndOfTurnResult _, EndOfTrickResult _, EndOfRoundResult _, TrickHistoryResult _ ->
                        AdapterResponse.toDomain(new ContinueCommand());

                case ParticipatingPlayersResult p -> {
                    List<Integer> indices = parser.parseNumbersInput(raw);
                    List<Player> players = indices.stream()
                            .map(i -> game.getPlayers().get(i - 1))
                            .toList();
                    yield AdapterResponse.toDomain(new PlayerListCommand(players));
                }

                // --- Play Card (The complex one) ---
                case PlayCardResult p -> {
                    int choice = parser.parseNumberInput(raw);

                    if (choice == 0) {
                        if (p.lastPlayedTrick() == null) {
                            yield AdapterResponse.uiOnly(new MessageIOEvent("No tricks have been played yet!"));
                        }
                        yield AdapterResponse.uiOnly(new TrickHistoryIOEvent(new TrickHistoryResult(p.lastPlayedTrick())));
                    }

                    if (choice < 1 || choice > p.currentPlayerHand().size()) {
                        yield AdapterResponse.uiOnly(new MessageIOEvent("Invalid selection. Please choose 0 to " + p.currentPlayerHand().size()));
                    }

                    Card selected = p.currentPlayerHand().get(choice - 1);
                    yield AdapterResponse.toDomain(new CardCommand(selected));
                }

                default -> throw new IllegalStateException("Unexpected GameResult in response handling: " + result);
            };
        }  catch (Exception e) {
        return AdapterResponse.uiOnly(new MessageIOEvent("Invalid input: \"" + raw + "\". Please try again."));
    }
    }
}