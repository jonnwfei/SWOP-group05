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
        this.currentRound = game.getRounds().getLast(); // will not throw since BidState ensures that a new Round has been instantiated
        this.currentTrick = new Trick(currentRound.getCurrentPlayer(), currentRound.getTrumpSuit());
    }

    /**
     * Handles the execution of playState, orchestrating the logic flow of a Round and its Tricks, in the give game
     * of which holds this state.
     *
     * @param input the Users response to the previous QuestionEvent
     * @return the next QuestionEvent or TextEvent
     */
    @Override
    public GameEvent executeState(String input) {
        Player currentPlayer = currentRound.getCurrentPlayer();
        String outputLog = "";

        if (input != null && !input.isEmpty() && currentPlayer.getRequiresConfirmation()) { // Checks if the input for HUMAN Turn
            try {

                int handIdx = Integer.parseInt(input);
                if (handIdx < 1 || handIdx > currentPlayer.getHand().size()) {
                    return new QuestionEvent("Invalid hand number. Choose between 1 and " + currentPlayer.getHand().size() + ":" );
                }

                Card playedCard = currentPlayer.getHand().get(handIdx -1);
                currentTrick.playCard(currentPlayer, playedCard); // plays the card and checks if the played card is valid, could throw
                currentRound.advanceToNextPlayer();
                outputLog = currentPlayer.getName() + " played " + playedCard.toString() + ".\n";
            } catch (NumberFormatException e) {
                return new QuestionEvent("Please enter a valid number:");
            } catch (IllegalArgumentException e) {
                return new QuestionEvent("Invalid move (" + e.getMessage() + "). Try again.");
            }

        } else if (!currentPlayer.getRequiresConfirmation()) { // Else it's a BOT Turn
            Card botCard = currentPlayer.chooseCard(currentTrick.getLeadingSuit());
            currentTrick.playCard(currentPlayer, botCard);
            currentRound.advanceToNextPlayer(); // This changes this Round's currentPlayer
            outputLog = currentPlayer.getName() + " played " + botCard.toString() + ".\n";
        }

        if (currentTrick.isCompleted()) { // Check if TRICK is completed
            Player winningPlayer = currentTrick.getWinningPlayer();
            currentRound.registerCompletedTrick(currentTrick);

            outputLog += "*** " + winningPlayer.getName() + " wins the trick! ***\n";

            if (currentRound.getTricks().size() >= Round.MAX_TRICKS) {
                outputLog += "\n --- ROUND OVER ---\nCalculating final scores...";

                return new TextEvent(outputLog);
            }
            this.currentTrick = new Trick(winningPlayer, currentRound.getTrumpSuit());
        }


        // Question event for the next Turn if nextPlayer is a Human
        Player nextPlayer = currentRound.getCurrentPlayer();
        if (nextPlayer.getRequiresConfirmation()) {
            String currentHand = nextPlayer.getFormattedHand();

            return new QuestionEvent(outputLog + "\nTrick: " + (currentRound.getTricks().size() + 1) +
                    " | " + nextPlayer.getName() + "'s turn.\n" +
                    "Your hand: \n" + currentHand + "\nChoose Card via index:");
        } else {
            return new TextEvent(outputLog);
        }
    }

    /**
     * Returns the nextState, after playState only comes MenuState or itself to continue on to the next Round within
     * the same game.
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