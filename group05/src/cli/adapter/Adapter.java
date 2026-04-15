package cli.adapter;

import base.domain.WhistGame;
import base.domain.bid.Bid;
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

    public AdapterResult handleResult(GameResult result) {
        return switch (result) {
            case PlayCardResult p -> {
                if (!p.player().getRequiresConfirmation()) {
                    Card chosen = p.player().chooseCard(
                            p.tableCards().isEmpty() ? null : p.tableCards().getFirst().suit()
                    );
                    yield new AdapterResult.Immediate(new CardCommand(chosen));
                }
                yield new AdapterResult.NeedsIO(new PlayCardIOEvent(p));
            }

            case BidTurnResult b -> {
                Player player = b.player();
                if (!player.getRequiresConfirmation()) {
                    Bid botBid = player.chooseBid();
                    // BidCommand with suit so domain skips SuitSelectionRequired
                    Suit chosenTrump = botBid.determineTrump(b.trumpSuit());
                    yield new AdapterResult.Immediate(new BidCommand(botBid.getType(), chosenTrump));
                }
                yield new AdapterResult.NeedsIO(new BidTurnIOEvent(b));
            }
            case SuitSelectionRequired ignored -> new AdapterResult.NeedsIO(new SuitSelectionIOEvent());
            case ProposalRejected p            -> new AdapterResult.NeedsIO(new ProposalRejectedIOEvent(p));
            case BiddingCompleted ignored      -> new AdapterResult.NeedsIO(new BiddingCompletedIOEvent());
            case BidSelectionResult b          -> new AdapterResult.NeedsIO(new BidSelectionIOEvent(b.availableBids()));
            case SuitSelectionResult ignore
                   -> new AdapterResult.NeedsIO(new SuitSelectionIOEvent());
            case PlayerSelectionResult p       -> new AdapterResult.NeedsIO(new PlayerSelectionIOEvent(p.players(), p.multiSelect()));
            case TrickInputResult ignored      -> new AdapterResult.NeedsIO(new TrickInputIOEvent());
            case ScoreBoardResult s            -> new AdapterResult.NeedsIO(new ScoreBoardIOEvent(s.names(), s.scores()));
            case SaveDescriptionResult ignored -> new AdapterResult.NeedsIO(new SaveDescriptionIOEvent());
            case ScoreBoardCompleteResult ignored -> new AdapterResult.NeedsIO(new ScoreBoardCompleteIOEvent());
            case EndOfTurnResult e             -> new AdapterResult.NeedsIO(new EndOfTurnIOEvent(e));
            case EndOfTrickResult e            -> new AdapterResult.NeedsIO(new EndOfTrickIOEvent(e));
            case EndOfRoundResult e            -> new AdapterResult.NeedsIO(new EndOfRoundIOEvent(e));
            case TrickHistoryResult t          -> new AdapterResult.NeedsIO(new TrickHistoryIOEvent(t));
            case ParticipatingPlayersResult p  -> new AdapterResult.NeedsIO(new ParticipatingPlayersIOEvent(p));
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
                    yield AdapterResponse.toDomain(
                            new BidCommand(b.availableBids().get(choice - 1))
                    );
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

                case ParticipatingPlayersResult _ -> {
                    List<Integer> indices = parser.parseNumbersInput(raw);
                    List<Player> players = indices.stream()
                            .map(i -> game.getPlayers().get(i - 1))
                           .toList();
                    yield AdapterResponse.toDomain(new PlayerListCommand(players));
                }
                                

                // --- Play Card
                case PlayCardResult p -> {
                    Player player = p.player();
                    //  BOT: auto-play
                    if (!player.getRequiresConfirmation()) {
                        Card chosen = player.chooseCard(p.tableCards().isEmpty() ? null : p.tableCards().getFirst().suit());

                        yield AdapterResponse.toDomain(new CardCommand(chosen));
                    }

                    // HUMAN: parse input
                    int choice = parser.parseNumberInput(raw);

                    if (choice == 0) {
                        if (p.lastPlayedTrick() == null) {
                            yield AdapterResponse.uiOnly(new MessageIOEvent("No tricks have been played yet!"));
                        }
                        yield AdapterResponse.uiOnly(
                                new TrickHistoryIOEvent(new TrickHistoryResult(p.lastPlayedTrick()))
                        );
                    }

                    if (choice < 1 || choice > p.legalCards().size()) {
                        yield AdapterResponse.uiOnly(
                                new MessageIOEvent("Invalid selection. Choose 0 to " + p.legalCards().size())
                        );
                    }

                    Card selected = p.legalCards().get(choice - 1);
                    yield AdapterResponse.toDomain(new CardCommand(selected));
                }

                default -> throw new IllegalStateException("Unexpected GameResult in response handling: " + result);
            };
        }  catch (Exception e) {
        return AdapterResponse.uiOnly(new MessageIOEvent("Invalid input: \"" + raw + "\". Please try again."));
    }
    }
}