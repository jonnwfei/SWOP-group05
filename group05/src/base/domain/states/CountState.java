package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.actions.NumberListAction;
import base.domain.events.ErrorEvent;
import base.domain.events.countEvents.*;
import base.domain.events.errorEvents.NumberListErrorEvent;
import base.domain.player.Player;
import base.domain.events.GameEvent;
import base.domain.bid.*;
import base.domain.card.Suit;
import base.domain.round.Round;
import java.util.ArrayList;
import java.util.List;
import static base.domain.bid.BidType.*;

/**
 * State responsible for manual score calculation. (use case 1)
 *
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class CountState extends State {

    private enum CountPhase {
        START, SELECT_BID, SELECT_TRUMP, SELECT_PLAYERS, CALCULATE, PROMPT_NEXT_STATE
    }

    private CountPhase currentPhase = CountPhase.START;
    private int numberBid;
    private int keuze;
    private Bid bid;
    private Suit trumpSuit;
    private List<Integer> participatingPlayers;

    public CountState(WhistGame game) {
        super(game);
    }

    /**
     * Routes the user action to the current phase of the scoring wizard.
     * @param action The user input.
     * @return The next event in the scoring sequence.
     */
    @Override
    public GameEvent<?> executeState(GameAction action) {
        return switch (currentPhase) {
            case START             -> handleStart();
            case SELECT_BID        -> handleSelectBid(action);
            case SELECT_TRUMP      -> handleSelectTrump(action);
            case SELECT_PLAYERS    -> handleSelectPlayers(action);
            case CALCULATE         -> handleCalculate(action);
            case PROMPT_NEXT_STATE -> handlePromptNextState(action);
        };
    }

    /** Initializes the wizard flow.
     * @return GameEvent
     * */
    private GameEvent<?> handleStart() {
        currentPhase = CountPhase.SELECT_BID;
        return new WelcomeCountEvent();
    }

    /** Processes the selected bid type (1-10).
     * @return GameEvent
     * */
    private GameEvent<?> handleSelectBid(GameAction action) {
        if (!(action instanceof NumberAction(int choice))) return null;

        if (choice < 1 || choice > 10) return new ErrorEvent(1, 10);

        this.numberBid = choice;
        if (choice == 7 || choice == 8) {
            currentPhase = CountPhase.SELECT_PLAYERS;
            return new PlayersInBidEvent(getPlayerNames());
        } else {
            currentPhase = CountPhase.SELECT_TRUMP;
            return new GetSuitEvent();
        }
    }

    /** Processes the trump suit selection.
     * @return GameEvent
     * */
    private GameEvent<?> handleSelectTrump(GameAction action) {
        if (!(action instanceof NumberAction(int choice))) return null;

        this.trumpSuit = switch (choice) {
            case 1 -> Suit.HEARTS;
            case 2 -> Suit.CLUBS;
            case 3 -> Suit.DIAMONDS;
            case 4 -> Suit.SPADES;
            default -> null;
        };

        if (this.trumpSuit == null) return new ErrorEvent(1, 4);

        currentPhase = CountPhase.SELECT_PLAYERS;
        return new PlayersInBidEvent(getPlayerNames());
    }

    /** Identifies which players were involved in the bid.
     * @return GameEvent
     * */
    private GameEvent<?> handleSelectPlayers(GameAction action) {
        if (!(action instanceof NumberListAction(ArrayList<Integer> indices))) return null;

        // Validation: Only Miserie allows multiple participants in this specific wizard flow
        if (numberBid != 7 && numberBid != 8 && indices.size() > 1) {
            PlayersInBidEvent tempEvent = new PlayersInBidEvent(getPlayerNames());
            return new NumberListErrorEvent(tempEvent::isValid);
        }

        this.participatingPlayers = indices;
        currentPhase = CountPhase.CALCULATE;
        return (numberBid == 7 || numberBid == 8) ? new MiserieWinnerEvent(getPlayerNames()) : new TrickWonEvent();
    }

    /** Performs final score calculation based on tricks won or miserie results.
     * @return GameEvent
     * */
    private GameEvent<?> handleCalculate(GameAction action) {
        List<Player> participants = participatingPlayers.stream()
                .map(idx -> getGame().getPlayers().get(idx - 1)).toList();

        createBidObject(participants.getFirst());
        Round round = new Round(getGame().getPlayers(), getGame().getPlayers().getFirst(), 1);
        round.setHighestBid(bid);
        getGame().addRound(round);

        if (numberBid == 7 || numberBid == 8) {
            if (!(action instanceof NumberListAction(ArrayList<Integer> winnerIndices))) return null;

            List<Player> winners;
            if (winnerIndices.size() == 1 && winnerIndices.get(0) == -1) {
                winners = new ArrayList<>();
            } else {
                for (int idx : winnerIndices) {
                    if (!participatingPlayers.contains(idx)) {
                        MiserieWinnerEvent tempEvent = new MiserieWinnerEvent(getPlayerNames());
                        return new NumberListErrorEvent(tempEvent::isValid);
                    }
                }
                winners = winnerIndices.stream().map(idx -> getGame().getPlayers().get(idx - 1)).toList();
            }
            round.calculateScoresForCount(0, participants, winners);
        } else {
            if (!(action instanceof NumberAction(int tricks)) || tricks < 0 || tricks > 13) {
                return new ErrorEvent(0, 13);
            }
            round.calculateScoresForCount(tricks, participants, null);
        }

        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        List<Integer> playerScores = getGame().getPlayers().stream().map(Player::getScore).toList();
        return new ScoreBoardEvent(getPlayerNames(), playerScores);
    }

    /** Final prompt to decide whether to count another round or exit.
     * @return GameEvent
     * */
    private GameEvent<?> handlePromptNextState(GameAction action) {
        if (!(action instanceof NumberAction(int choice)) || choice < 1 || choice > 2) {
            return new ErrorEvent(1, 2);
        }
        this.keuze = choice;
        return new EndOfCountStateEvent();
    }

    /** Helper to instantiate the correct Bid object for score calculation. */
    private void createBidObject(Player bidder) {
        this.bid = switch (numberBid) {
            case 1 -> new SoloProposalBid(bidder);
            case 2 -> new ProposalBid(bidder);
            case 3 -> new AbondanceBid(bidder, ABONDANCE_9, trumpSuit);
            case 4 -> new AbondanceBid(bidder, ABONDANCE_10, trumpSuit);
            case 5 -> new AbondanceBid(bidder, ABONDANCE_11, trumpSuit);
            case 6 -> new AbondanceBid(bidder, ABONDANCE_12_OT, trumpSuit);
            case 7 -> new MiserieBid(bidder, MISERIE);
            case 8 -> new MiserieBid(bidder, OPEN_MISERIE);
            case 9 -> new SoloBid(bidder, SOLO, trumpSuit);
            case 10 -> new SoloBid(bidder, SOLO_SLIM, trumpSuit);
            default -> throw new IllegalStateException("Invalid bid index");
        };
    }

    private List<String> getPlayerNames() {
        return getGame().getPlayers().stream().map(Player::getName).toList();
    }

    /** Returns to a fresh CountState or the Main Menu. */
    @Override
    public State nextState(){
        return (keuze == 1) ? new CountState(getGame()) : new MenuState(getGame());
    }
}