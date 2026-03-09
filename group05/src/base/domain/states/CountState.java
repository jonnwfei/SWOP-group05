package base.domain.states;

import base.domain.WhistGame;
import base.domain.player.Player;
import cli.elements.GameEvent;
import cli.elements.QuestionEvent;
import cli.elements.TextEvent;
import base.domain.bid.*;
import base.domain.card.Suit;
import base.domain.round.Round;
import java.util.Arrays;
import java.util.List;
import static base.domain.bid.BidType.*;

public class CountState extends State {

    private enum CountPhase {
        START, SELECT_BID, SELECT_TRUMP, SELECT_PLAYERS, CALCULATE, FINISH
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
     *Executes the count state
     *
     * @param  input the users response to the previous QuestionEvent
     * @return the next QuestionEvent or TextEvent
     * @throws IllegalStateException getting in a unknown state
     */
    @Override
    public GameEvent executeState(String input) {
        try {
            return switch (currentPhase) {
                case START          -> handleStart();
                case SELECT_BID     -> handleSelectBid(input);
                case SELECT_TRUMP   -> handleSelectTrump(input);
                case SELECT_PLAYERS -> handleSelectPlayers(input);
                case CALCULATE     -> handleCalculate(input);
                case FINISH        -> handleFinish(input);
            };
        } catch (NumberFormatException e) {
            return new QuestionEvent("That's not a valid number. Please try again:");
        } catch (Exception e) {
            System.err.println("Unexpected error in " + currentPhase + ": " + e.getMessage());
            return new QuestionEvent("An error occurred. Please repeat your last input:");
        }
    }

    // --- HELPER METHODS ---
    /**
     * First questionEvent the Count State
     *
     * @return QuestionEvent what bid was played
    * */
    private GameEvent handleStart() {
        String msg = """
                ===== WELCOME TO THE COUNT ====\s
                 WHICH ROUND WAS PLAYED?\s
                Proposal:\s
                (1) Alone    (2) With Partner
                Abondance:
                (3) 9   (4) 10   (5) 11   (6) 12
                Miserie:
                (7) Normal       (8) Open
                Solo:
                (9) Normal       (10) Solo Slim
                """;
        currentPhase = CountPhase.SELECT_BID;
        return new QuestionEvent(msg);
    }
    /**
     * Second questionEvent the Count State
     * handles de bid, and asks what the trump is
     * @param  input  answer of the user to what bid is played
     * @return QuestionEvent what is the trump suit
     *
     * */
    private GameEvent handleSelectBid(String input) {
        int choice = Integer.parseInt(input);
        if (choice < 1 || choice > 10) return new QuestionEvent("Invalid choice (1-10):");
        this.numberBid = choice;
        currentPhase = CountPhase.SELECT_TRUMP;
        return new QuestionEvent("What Suit is the trump suit?\n(1) Hearts (2) Clubs (3) Diamonds (4) Spades");
    }

    /**
     * Third questionEvent the Count State
     * Handles the trump and asks for the players who played the bid
     * @param  input  answer from user what suit trump is
     * @return QuestionEvent which players played the bid
     * */
    private GameEvent handleSelectTrump(String input) {
        int choice = Integer.parseInt(input);
        this.trumpSuit = switch (choice) {
            case 1 -> Suit.HEARTS; case 2 -> Suit.CLUBS;
            case 3 -> Suit.DIAMONDS; case 4 -> Suit.SPADES;
            default -> null;
        };
        if (this.trumpSuit == null) return new QuestionEvent("Invalid suit (1-4):");
        currentPhase = CountPhase.SELECT_PLAYERS;
        return new QuestionEvent("Which player numbers played this bid?\n" + getGame().printNames());
    }
    /**
     * Handles the players that played in the bid, asks depending on the bid the amount of tricks of which players won
     * @param  input  answer from user to which players were playing this bid
     * @return QuestionEvent who won the bid or how many tricks were won
    * */
    private GameEvent handleSelectPlayers(String input) {
        this.participatingPlayers = parseIndices(input);
        if (participatingPlayers.isEmpty()) {
            return new QuestionEvent("Select at least one player:");
        }
        for (int idx : participatingPlayers) {
            if (idx < 0 || idx >= getGame().getPlayers().size()) {
                return new QuestionEvent("Invalid player index: " + idx);
            }
        }
        if (numberBid == 2 && participatingPlayers.size() != 2) {
            return new QuestionEvent("Select exactly two players:\n" + getGame().printNames());
        }
        else if (numberBid != 2&&numberBid != 7 && numberBid !=8 && participatingPlayers.size() != 1){
            return new QuestionEvent("Select exactly one player:\n" + getGame().printNames());
        }
        currentPhase = CountPhase.CALCULATE;
        //going to next bid confirmed
        if (numberBid == 7 || numberBid == 8) {
            return new QuestionEvent("Which players won their bid? (Got 0 tricks): \n" + getGame().printNames());
        }

        return new QuestionEvent("How many tricks did the player(s) win?");
    }

    /**
     *Handles the results of the bid and calculates the scores, prints them and asks what state to go next to
     * @param input answer from user to question who won the bid or how many tricks were won
     * @return QuestionEvent about if the users wants to count another round
     */
    private GameEvent handleCalculate(String input) {
        //prep
        List<Player> participants = participatingPlayers.stream()
                .map(idx -> getGame().getPlayers().get(idx)).toList();

        createBidObject(participants.getFirst());
        Round round = new Round(getGame().getPlayers(), getGame().getPlayers().getFirst(), 1);
        round.setHighestBid(bid);
        getGame().addRound(round);
        //case of miserie
        if (numberBid == 7 || numberBid == 8) {
            List<Integer> winnerIndices = parseIndices(input);
            if (winnerIndices.isEmpty()) {
                return new QuestionEvent("Select at least one winning player:");
            }
            for (int idx : winnerIndices) {
                if (idx < 0 || idx >= getGame().getPlayers().size()) {
                    return new QuestionEvent("Invalid player index: " + idx);
                }
                if (!participatingPlayers.contains(idx)) {
                    return new QuestionEvent("Winners must be among the participating players:");
                }
            }
            List<Player> winners = winnerIndices.stream().map(idx -> getGame().getPlayers().get(idx)).toList();
            round.calculateScoresForCount(0, participants, winners);
        }//other cases
        else {
            int tricks = Integer.parseInt(input);
            if (tricks < 0 || tricks > 13) return new QuestionEvent("Tricks must be 0-13:");
            round.calculateScoresForCount(tricks, participants, null);
        }

        currentPhase = CountPhase.FINISH;
        return new QuestionEvent(getGame().printScore() + "\n" +
                "Do you want to: \n(1) Simulate another round\n(2) Go back to the main menu");
    }

    /**
     * Defensive programming for final state
     * @param input wrong input for nextState
     * @return QuestionEvent about what to do next
     */
    private GameEvent handleFinish(String input) {
        keuze = Integer.parseInt(input);
        if (keuze != 1 && keuze != 2) return new QuestionEvent("Choose (1) or (2):");
        return new TextEvent("Round finalized.");
    }

    // --- UTILS ---

    /**
     * Splits up the input to get the different numbers that were put in
     * @param input (eg. 1,4)
     * @return list containing those numbers (eg. [1,4])
     */
    private List<Integer> parseIndices(String input) {
        return Arrays.stream(input.split("[^0-9]+"))
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .map(i -> i - 1)
                .toList();
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

    @Override

    public State nextState(){
        return (keuze == 1) ? new CountState(getGame()) : new MenuState(getGame());
    }

}
