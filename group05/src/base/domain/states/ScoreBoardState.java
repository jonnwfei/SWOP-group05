package base.domain.states;

import base.domain.WhistGame;
import base.domain.player.Player;
import cli.elements.GameEvent;
import cli.elements.QuestionEvent;
import cli.elements.TextEvent;

/**
 * EndRoundState, mini menu state where the player can restart Round (12a)
 *
 * @author John Cai
 * @since 02/03/2026
 */
public class EndRoundState extends State {
    private boolean userWantsToQuit = false;
    private boolean userWantsToRestart = false;

    public EndRoundState(WhistGame game) {
        super(game);
        game.getCurrentRound().calculateScores(); // TODO: fix getCurrentRound to return null if empty round, cuz curently it throws
    }

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

    @Override
    public State nextState() {
        if (userWantsToQuit) {
            return new MenuState(getGame());
        } else if (userWantsToRestart) {
            //
            return new BidState(getGame());
        } else {
            return this;
        }
    }
}
