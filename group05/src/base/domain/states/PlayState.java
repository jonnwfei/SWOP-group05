package base.domain.states;

import base.domain.WhistGame;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.trick.Trick;
import base.domain.trick.Turn;
import cli.elements.GameEvent;
import cli.elements.QuestionEvent;
import cli.elements.TextEvent;


/**
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class PlayState extends State {
    private final Round currentRound;
    private Trick currentTrick;
    private boolean isHandHidden;
    private boolean firstTurn = true;


    /**
     * Instantiates the playState.
     *
     * @param game the given game that holds this State
     */
    public PlayState(WhistGame game) {
        super(game);
        Round round = game.getCurrentRound();
        if (round == null) {
            throw new IllegalStateException("Cannot create PlayState: no currentRound exist; make sure a round has been created.");
        }
        this.currentRound = round; // will not throw since BidState ensures that a new Round has been instantiated
        this.currentTrick = new Trick(currentRound.getCurrentPlayer(), currentRound.getTrumpSuit());
        this.isHandHidden = false;
    }

    /**
     * Handles the execution of playState, orchestrating the logic flow of a Round and its Tricks, in the given game
     * that holds this state.
     *
     * @param input the user's response to the previous QuestionEvent
     * @return the next QuestionEvent or TextEvent
     */
    @Override
    public GameEvent executeState(String input) {
        Player currentPlayer = currentRound.getCurrentPlayer();
        StringBuilder outputLog = new StringBuilder();

        // Initial Load
        if (input == null || firstTurn) {
            isHandHidden = true;
            firstTurn = false;
            return new QuestionEvent("\n============== Pass the terminal to " + currentPlayer.getName() + " ==============\n"
                                        + "Press ANY BUTTON to reveal your hand...");
        }
        // User pressed "ANY BUTTON" on the Pass terminal screen
        if (isHandHidden) {
            isHandHidden = false;

            String currentHand = currentPlayer.getFormattedHand(true);
            return new QuestionEvent(buildTableDisplay() + "\nTrick: " + (currentRound.getTricks().size() + 1) +
                    " | " + currentPlayer.getName() + "'s turn.\n" + "(0) to show last played Trick.\n" +
                    "Your hand: \n" + currentHand + "\nChoose Card via index:");
        }

        // Checks if the input for HUMAN Turn
        if (currentPlayer.getRequiresConfirmation()) {
            try {
                int handIdx = Integer.parseInt(input);
                if (handIdx < 0 || handIdx > currentPlayer.getHand().size()) {
                    return new QuestionEvent("Invalid hand number\nChoose (0) to see the last trick or between 1 and " + currentPlayer.getHand().size() + ": ");
                }
                if (handIdx == 0) {
                    String lastPlayedTrick = buildLastTrickDisplay();
                    String display = lastPlayedTrick == null ? "No last played trick has been found.\n" : lastPlayedTrick;
                    return new QuestionEvent(display + "\nChoose Card via index:");
                }

                Card playedCard = currentPlayer.getHand().get(handIdx - 1);
                currentTrick.playCard(currentPlayer, playedCard); // plays the card and checks if the played card is valid, could throw
                outputLog.append(currentPlayer.getName()).append(" played ").append(playedCard.toString()).append(".\n");

                isHandHidden = true; // Hide the hand for next player

                TextEvent roundCompletedEvent = processTurnOutcome(outputLog);
                if (roundCompletedEvent != null) {
                    return roundCompletedEvent; // Ends the State if the round is over.
                }

            } catch (NumberFormatException e) {
                return new QuestionEvent("Invalid hand number\nChoose (0) to see the last trick or between 1 and " + currentPlayer.getHand().size() + ":");
            } catch (IllegalArgumentException e) {
                return new QuestionEvent("Invalid move, '" + e.getMessage() + "',\n(Try again!)\nChoose Card via index:");
            }
        }
        // Updates the reference pointer if HUMAN just played
        currentPlayer = currentRound.getCurrentPlayer();

        // Loop as long as the next players are BOTS
        while (!currentPlayer.getRequiresConfirmation()) { // Else it's a BOT Turn
            Card botCard = currentPlayer.chooseCard(currentTrick.getLeadingSuit());
            currentTrick.playCard(currentPlayer, botCard);
            outputLog.append(currentPlayer.getName()).append(" played ").append(botCard.toString()).append(".\n");

            TextEvent roundCompletedEvent = processTurnOutcome(outputLog);
            if (roundCompletedEvent != null) {
                return roundCompletedEvent; // Ends the State if the round is over.
            }

            currentPlayer = currentRound.getCurrentPlayer(); // Updates the reference pointer to the next BOT
        }

        // Question event for the next Turn if nextPlayer is a Human
        return new QuestionEvent(outputLog
                + "\n\n============== Pass the terminal to " + currentRound.getCurrentPlayer().getName() + " ==============\n"
                + "Press ANY BUTTON to reveal your hand...\n");

    }

    /**
     * Helper method to process the outcome of a Turn. As it checks if a Trick or Round has ended.
     *
     * @param outputLog the game HistoryLog
     * @return TextEvent if the Round is over, signaling a nextState to be called, else returns null.
     */
    private TextEvent processTurnOutcome(StringBuilder outputLog) {
        if (currentTrick.isCompleted()) { // Check if TRICK is completed
            Player winningPlayer = currentTrick.getWinningPlayer();
            currentRound.registerCompletedTrick(currentTrick);

            outputLog.append("\n============== ").append(winningPlayer.getName()).append(" wins the trick! ==============\n");

            if (currentRound.getTricks().size() >= Round.MAX_TRICKS) { // We check if Round has already completed 13 tricks
                outputLog.append("\n============== ROUND OVER ==============\nCalculating final scores...\n");
                return new TextEvent(outputLog.toString());
            }
            outputLog.append("\n============== NEXT TRICK ==============\n");
            this.currentTrick = new Trick(winningPlayer, currentRound.getTrumpSuit());
        } else {
            currentRound.advanceToNextPlayer();
        }
        return null; // Signals that the Round Continues
    }

    /**
     * Builds a string containing the currently Played cards and if applicable, the Hand of the player playing OPEN MISERIE
     *
     * @return formatted string that holds the current table status of open cards
     */
    private String buildTableDisplay() {
        StringBuilder table = new StringBuilder("\n-------------- CARDS ON TABLE ---------------\n");
        if (currentTrick.getTurns().isEmpty()) {
            table.append("(No cards played yet)\n");
        } else {
            for (Turn turn : currentTrick.getTurns()) {
                table.append("- ").append(turn.toString()).append("\n");
            }
        }

        Bid highestBid = currentRound.getHighestBid();
        if (highestBid != null && highestBid.getType() == BidType.OPEN_MISERIE) {
            Player exposedPlayer = highestBid.getPlayer();
            table.append("\n--- EXPOSED HAND (OPEN_MISERIE: ").append(exposedPlayer.getName()).append(")---\n");
            table.append(exposedPlayer.getFormattedHand()).append("\n");
        }
        table.append("---------------------------------------------\n");
        return table.toString();
    }

    /**
     * Builds a string containing the last played trick
     *
     * @return formatted string that shows the last Played Trick if any were found.
     */
    private String buildLastTrickDisplay() {
        Trick lastTrick = getGame().getCurrentRound().getLastPlayedTrick();
        if (lastTrick == null) return null;

        StringBuilder table = new StringBuilder("\n-------------- LAST PLAYED TRICK ---------------\n");

        for (Turn turn : lastTrick.getTurns()) {
            table.append("- ").append(turn.toString()).append("\n");
        }
        table.append("------------------------------------------------\n");
        return table.toString();
    }

    /**
     * Returns the nextState. After playState, the game either transitions to a new MenuState when the current Round has
     * finished, or returns this same playState instance so that the current round can continue.
     *
     * @return the next state
     */
    @Override
    public State nextState() {
        if (currentRound.getTricks().size() >= Round.MAX_TRICKS) {
            return new ScoreBoardState(this.getGame(), new BidState(getGame()));
        }
        return this;
    }
}