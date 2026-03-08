package base.domain.states;

import base.domain.WhistGame;

import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.trick.Trick;
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

        if (input != null && !input.isEmpty() && currentPlayer.getRequiresConfirmation()) { // Checks if the input for HUMAN Turn
            try {
                int handIdx = Integer.parseInt(input);
                if (handIdx < 1 || handIdx > currentPlayer.getHand().size()) {
                    return new QuestionEvent("Invalid hand number. Choose between 1 and " + currentPlayer.getHand().size() + ":");
                }

                Card playedCard = currentPlayer.getHand().get(handIdx - 1);
                currentTrick.playCard(currentPlayer, playedCard); // plays the card and checks if the played card is valid, could throw
                outputLog.append(currentPlayer.getName()).append(" played ").append(playedCard.toString()).append(".\n");

                TextEvent roundCompletedEvent = processTurnOutcome(outputLog);
                if (roundCompletedEvent != null) {
                    return roundCompletedEvent; // Ends the State if the round is over.
                }

            } catch (NumberFormatException e) {
                return new QuestionEvent("Please enter a valid number:");
            } catch (IllegalArgumentException e) {
                return new QuestionEvent("Invalid move (" + e.getMessage() + "). Try again.");
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
        String currentHand = currentPlayer.getFormattedHand();

        return new QuestionEvent(outputLog + "\nTrick: " + (currentRound.getTricks().size() + 1) +
                " | " + currentPlayer.getName() + "'s turn.\n" +
                "Your hand: \n" + currentHand + "\nChoose Card via index:");

    }

    /**
     * Helper method to process the outcome of a Turn. As it checks if a Trick or Round has ended.
     * @param outputLog the game HistoryLog
     * @return TextEvent if the Round is over, signaling a nextState to be called, else returns null.
     */
    private TextEvent processTurnOutcome(StringBuilder outputLog) {
        if (currentTrick.isCompleted()) { // Check if TRICK is completed
            Player winningPlayer = currentTrick.getWinningPlayer();
            currentRound.registerCompletedTrick(currentTrick);

            outputLog.append("*** ").append(winningPlayer.getName()).append(" wins the trick! ***\n");

            if (currentRound.getTricks().size() >= Round.MAX_TRICKS) { // We check if Round has already completed 13 tricks
                outputLog.append("\n --- ROUND OVER ---\nCalculating final scores...");
                return new TextEvent(outputLog.toString());
            }
            this.currentTrick = new Trick(winningPlayer, currentRound.getTrumpSuit());
        } else {
            currentRound.advanceToNextPlayer();
        }
        return null; // Signals that the Round Continues
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
            return new MenuState(getGame());
        }
        return this;
    }
}