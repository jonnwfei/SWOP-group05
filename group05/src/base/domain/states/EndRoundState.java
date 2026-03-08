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

        game.getCurrentRound().calculateScores();
    }

    @Override
    public GameEvent executeState(String input) {
        if (input != null && !input.isEmpty()) {
            if (input.equals("0")) {
                userWantsToRestart = true;
                return new TextEvent("Starting new round...");
            } else if (input.equals("1")) {
                userWantsToQuit = true;
                return new TextEvent("Returning to main menu...");
            } else {
                return new QuestionEvent("Invalid input. (0) Next Round or (1) Quit: ");
            }
        }

        StringBuilder scoreBoard = new StringBuilder("\n---ROUND OVER: Final Scores ---\n");
        for (Player p : getGame().getPlayers()) {
            scoreBoard.append(p.getName()).append(": ").append(p.getScore()).append(" points\n");
        }
        scoreBoard.append("-------------------------------\n");
        scoreBoard.append("(0) Start next round\n (1) Quit to Menu\nYour Choice: ");
        return new QuestionEvent(scoreBoard.toString());
    }

    @Override
    public State nextState() {
        if (userWantsToQuit) {
            return new MenuState(getGame());
        } else if (userWantsToRestart) {
            return new BidState(getGame());
        } else {
            return this;
        }
    }
}
