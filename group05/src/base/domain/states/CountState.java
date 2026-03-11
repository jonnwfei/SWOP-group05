package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.actions.NumberListAction;
import base.domain.actions.TextAction;
import base.domain.events.ErrorEvent;
import base.domain.events.countEvents.*;
import base.domain.player.Player;
import base.domain.events.GameEvent;
import base.domain.bid.*;
import base.domain.card.Suit;
import base.domain.round.Round;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static base.domain.bid.BidType.*;

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
     * Executes the count state
     *
     * @param action@return the next QuestionEvent or TextEvent
     * @throws IllegalStateException getting in an unknown state
     */
    @Override
    public GameEvent executeState(GameAction action) {
        return switch (currentPhase) {
            case START             -> handleStart();
            case SELECT_BID        -> handleSelectBid(action);
            case SELECT_TRUMP      -> handleSelectTrump(action);
            case SELECT_PLAYERS    -> handleSelectPlayers(action);
            case CALCULATE         -> handleCalculate(action);
            case PROMPT_NEXT_STATE -> handlePromptNextState(action);
        };
    }
    // --- HELPER METHODS ---
    /**
     * First questionEvent the Count State
     *
     * @return QuestionEvent what bid was played
    * */
    private GameEvent handleStart() {
        currentPhase = CountPhase.SELECT_BID;
        return new WelcomeCountEvent();
    }
    /**
     * Second questionEvent the Count State
     * handles de bid, and asks what the trump is
     * @param action of the user to what bid is played
     * @return QuestionEvent what is the trump suit
     *
     * */
    private GameEvent handleSelectBid(GameAction action) {
        if (!(action instanceof NumberAction(int choice))) {
            return null; //illegalActionTypeEvent
        }

        if (choice < 1 || choice > 10) {
            return new ErrorEvent(1, 10);
        }

        this.numberBid = choice;
        currentPhase = CountPhase.SELECT_TRUMP;
        return new GetSuitEvent();
    }

    /**
     * Third questionEvent the Count State
     * Handles the trump and asks for the players who played the bid
     * @param  action  answer from user what suit trump is
     * @return QuestionEvent which players played the bid
     * */
    private GameEvent handleSelectTrump(GameAction action) {
        if (!(action instanceof NumberAction(int choice))) {
            return null; //illegalActionTypeEvent
        }

        this.trumpSuit = switch (choice) {
            case 1 -> Suit.HEARTS;
            case 2 -> Suit.CLUBS;
            case 3 -> Suit.DIAMONDS;
            case 4 -> Suit.SPADES;
            default -> null;
        };

        if (this.trumpSuit == null) {
            return new ErrorEvent(1, 4);
        }

        currentPhase = CountPhase.SELECT_PLAYERS;
        return new PlayersInBidEvent(getPlayerNames());
    }
    /**
     * Handles the players that played in the bid, asks depending on the bid the amount of tricks of which players won
     * @param  action  answer from user to which players were playing this bid
     * @return QuestionEvent who won the bid or how many tricks were won
    * */
    private GameEvent handleSelectPlayers(GameAction action) {
        if (!(action instanceof NumberListAction(ArrayList<Integer> indices) )) {
            return null; //IllegalActionEvent
        }
        this.participatingPlayers = indices;
        currentPhase = CountPhase.CALCULATE;
        //going to next bid confirmed
        if (numberBid == 7 || numberBid == 8) {
            return new MiserieWinnerEvent(getPlayerNames());

        }
        return new TrickWonEvent();
    }

    /**
     *Handles the results of the bid and calculates the scores, prints them and asks what state to go next to
     * @param action answer from user to question who won the bid or how many tricks were won
     * @return QuestionEvent about if the users wants to count another round
     */
    private GameEvent handleCalculate(GameAction action) {
        List<Player> participants = participatingPlayers.stream()
                .map(idx -> getGame().getPlayers().get(idx - 1)).toList();

        createBidObject(participants.getFirst());
        Round round = new Round(getGame().getPlayers(), getGame().getPlayers().getFirst(), 1);
        round.setHighestBid(bid);
        getGame().addRound(round);

        // Case of Miserie (7 or 8)
        if (numberBid == 7 || numberBid == 8) {
            if (!(action instanceof NumberListAction(ArrayList<Integer> winnerIndices))) {
                return null;
            }

            List<Player> winners;

            // Check for the "Everyone Lost" special case (-1)
            if (winnerIndices.size() == 1 && winnerIndices.get(0) == -1) {
                winners = new ArrayList<>(); // Empty list means no winners, so everyone in participants loses
            } else {
                // Standard validation: check if indices exist in the current participants
                for (int idx : winnerIndices) {
                    if (!participatingPlayers.contains(idx)) {
                        return new ErrorEvent(1, getGame().getPlayers().size());
                    }
                }
                // Map 1-based indices to 0-based objects
                winners = winnerIndices.stream()
                        .map(idx -> getGame().getPlayers().get(idx - 1))
                        .toList();
            }

            round.calculateScoresForCount(0, participants, winners);
        }
        // Other bids (Tricks won)
        else {
            if (!(action instanceof NumberAction(int tricks))) {
                return new ErrorEvent(0, 13);
            }
            if (tricks < 0 || tricks > 13) {
                return new ErrorEvent(0, 13);
            }
            round.calculateScoresForCount(tricks, participants, null);
        }

        currentPhase = CountPhase.PROMPT_NEXT_STATE;

        List<Integer> playerScores = getGame().getPlayers().stream().map(Player::getScore).toList();
        return new ScoreBoardEvent(getPlayerNames(), playerScores);
    }

    /**
     * Handles the prompt that asks: [1] Count Again, [2] Menu
     */
    private GameEvent handlePromptNextState(GameAction action) {
        if (!(action instanceof NumberAction(int choice))) {
            return new ErrorEvent(1, 2);
        }

        if (choice < 1 || choice > 2) {
            return new ErrorEvent(1, 2);
        }

        // Successfully saved the user's choice!
        this.keuze = choice;

        // Return an event to break the inner UI loop (you might have a MessageEvent or EndOfStateEvent for this)
        return new EndOfCountStateEvent(); // Adjust this return to whatever breaks your Controller loop!
    }

    /**
     * creates the bid depending on numberBid and the player thats given (for less code cluttering seperated)
     * @param bidder player that plays the bid
     *
     */
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


    @Override
    public State nextState(){
        return (keuze == 1) ? new CountState(getGame()) : new MenuState(getGame());
    }

}
