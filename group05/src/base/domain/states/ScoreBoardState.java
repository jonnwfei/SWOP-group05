package base.domain.states;

import base.domain.WhistGame;
import cli.elements.GameEvent;
import cli.elements.QuestionEvent;
import cli.elements.TextEvent;

/**
 * EndRoundState, mini menu state where the player can restart Round (12a)
 *
 * @author John Cai
 * @since 02/03/2026
 */
public class ScoreBoardState extends State {
    private boolean userWantsToQuit = false;
    private boolean userWantsToRestart = false;
    private final State targetRestartState; // Can hold either new BidState or CountState

    /**
     * Constructs a ScoreBordState for prompting the player whether to restart or quit to the menu. (Can be used for either
     * PlayState and CountState, avoiding double code)
     * @param game that will hold this state
     * @param targetRestartState can either be new CountState or new BidState
     */
    public ScoreBoardState(WhistGame game, State targetRestartState) {
        super(game);
        game.getCurrentRound().calculateScores(); // TODO: fix getCurrentRound to return null if empty round, cuz curently it throws
        this.targetRestartState = targetRestartState;
    }

    /**
     *
     * @param input the user's response on the previous QuestionEvent
     * @return GameEvent either TextEvent which calls nextState, or QuestionEvent which returns the currentState
     */
    @Override
    public GameEvent executeState(String input) {
        if (input != null && !input.isEmpty()) {
            if (input.equals("1")) {
                userWantsToRestart = true;
                return new TextEvent("Starting new round...");
            } else if (input.equals("2")) {
                userWantsToQuit = true;
                return new TextEvent("Returning to main menu...");
            } else {
                return new QuestionEvent("Invalid input. (0) Next Round or (1) Quit: ");
            }
        }

        String prompt = getGame().printScore() + "\n\n" +
                "Do you want to:\n\n" +
                "(1) Play another round\n" +
                "(2) Quit to main menu\n" +
                "Your choice: ";
        return new QuestionEvent(prompt);
    }

    /**
     * Method to transition onwards to the nextState, following a ScoreBoardState can either be a MenuState, CountState
     * or BidState.
     *
     * @return nextState
     */
    @Override
    public State nextState() {
        if (userWantsToQuit) {
            return new MenuState(getGame());
        } else if (userWantsToRestart) {
            // TODO: needs to setup game, as to set the nextDealer as the person next to the dealer/ this is done by bidState

            return targetRestartState;
        } else {
            return this;
        }
    }
}
