package cli;

import base.domain.WhistGame;
import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.results.*;
import cli.elements.Response;
import cli.events.*;
import cli.events.CountEvents.*;
import cli.events.BidEvents.*;
import cli.events.PlayEvents.*;

import java.util.List;

public class Adapter {

    private final TerminalParser parser;
    private WhistGame game;
    public Adapter(WhistGame game) {

        this.parser = new TerminalParser();
        this.game = game;
    }

    public IOEvent handleResult(GameResult result) {
        return switch (result) {
            case BidTurnResult b               -> new BidTurnIOEvent(b);
            case SuitSelectionRequired ignored      -> new SuitSelectionIOEvent();
            case ProposalRejected p            -> new ProposalRejectedIOEvent(p);
            case BiddingCompleted ignored      -> new BiddingCompletedIOEvent();
            case BidSelectionResult b          -> new BidSelectionIOEvent(b.availableBids());
            case SuitSelectionResult s   -> new SuitSelectionIOEvent();
            case PlayerSelectionResult p       -> new PlayerSelectionIOEvent(p.players(), p.multiSelect());
            case TrickInputResult ignored      -> new TrickInputIOEvent();
            case ScoreBoardResult s            -> new ScoreBoardIOEvent(s.names(), s.scores());
            case SaveDescriptionResult ignored -> new SaveDescriptionIOEvent();
            case ScoreBoardCompleteResult ignored -> new ScoreBoardCompleteIOEvent();
            case PlayCardResult p -> new PlayCardIOEvent(p);
            case EndOfTurnResult e           -> new EndOfTurnIOEvent(e);
            case EndOfTrickResult e          -> new EndOfTrickIOEvent(e);
            case EndOfRoundResult e          -> new EndOfRoundIOEvent(e);
            case TrickHistoryResult t        -> new TrickHistoryIOEvent(t);
            case ParticipatingPlayersResult p -> new ParticipatingPlayersIOEvent(p);
            default -> throw    new IllegalStateException("Unexpected value: " + result);
        };
    }

    public GameCommand handleResponse(Response response, GameResult result) {
        if (response.rawInput() == null) return new ContinueCommand();
        String raw = response.rawInput();

        return switch (result) {
            // --- bid state ---
            case BidTurnResult b -> {
                int choice = parser.parseNumberInput(raw);
                yield new BidCommand(b.availableBids()[choice - 1]);
            }

            case SuitSelectionRequired ignored -> {
                int choice = parser.parseNumberInput(raw);
                yield new SuitCommand(Suit.values()[choice - 1]);
            }

            case ProposalRejected ignored -> {
                int choice = parser.parseNumberInput(raw);
                yield new BidCommand(choice == 1 ? BidType.PASS : BidType.SOLO_PROPOSAL);
            }

            case BiddingCompleted ignored -> new ContinueCommand();

            // --- count state ---
            case BidSelectionResult b -> {
                int choice = parser.parseNumberInput(raw);
                yield new BidCommand(b.availableBids()[choice - 1]);
            }

            case SuitSelectionResult ignored -> {
                int choice = parser.parseNumberInput(raw);
                yield new SuitCommand(Suit.values()[choice - 1]);
            }

            case PlayerSelectionResult ignored -> {
                String trimmed = raw.trim();
                if (trimmed.equals("0")) {
                    yield new PlayerListCommand(List.of()); // empty list
                }
                List<Integer> indices = parser.parseNumbersInput(trimmed);
                List<Player> players = indices.stream()
                        .map(i -> game.getPlayers().get(i - 1))
                        .toList();
                yield new PlayerListCommand(players);
            }

            case TrickInputResult ignored -> {

                int tricks = parser.parseNumberInput(raw);
                yield new NumberCommand(tricks);
            }

            case ScoreBoardResult ignored -> {
                int choice = parser.parseNumberInput(raw);
                yield new NumberCommand(choice);
            }

            case SaveDescriptionResult ignored -> {
                String text = parser.parseString(raw);
                yield new TextCommand(text);
            }
            case EndOfTurnResult ignored     -> new ContinueCommand();
            case EndOfTrickResult ignored    -> new ContinueCommand();
            case EndOfRoundResult ignored    -> new ContinueCommand();
            case TrickHistoryResult ignored  -> new ContinueCommand();
            case ParticipatingPlayersResult p -> {
                List<Integer> indices = parser.parseNumbersInput(raw);
                List<Player> players = indices.stream()
                        .map(i -> game.getPlayers().get(i - 1))
                        .toList();
                yield new PlayerListCommand(players);
            }
            case PlayCardResult p -> {
                int choice = parser.parseNumberInput(raw);
                if (choice == 0) yield new NumberCommand(0); // show last trick
                yield new CardCommand(p.currentPlayerHand().get(choice - 1));
            }
            default -> throw new IllegalStateException("Unexpected value: " + result);
        };
    }
}