package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.events.countEvents.ScoreBoardEvent;
import base.domain.events.playevents.ScoreBoardCompleteEvent;
import base.domain.player.Player;
import java.util.List;

/**
 * Handles the end-of-round scoreboard display and provides options for where
 * they can go next.
 * 
 * @author John Cai
 * @since 09/03/2026
 */
public class ScoreBoardState extends State {

    private int choice = 0; // 0 = undecided, 1 = restart, 2 = quit

    /**
     * Initializes the scoreboard state.
     * 
     * @param game The current game instance.
     */
    public ScoreBoardState(WhistGame game) {
        super(game);
    }

    /**
     * Processes the scoreboard interaction.
     * 
     * @param action The user action
     * @return a GameEvent
     */
    @Override
    public GameEvent<?> executeState(GameAction action) {
        switch (action) {
            case NumberAction(int input) -> {
                if (input == 1 || input == 2) {
                    this.choice = input;
                    return new ScoreBoardCompleteEvent();
                }
                return new ErrorEvent(1, 2);
            }
            default -> {
                // Initial entry: gather data and show the scoreboard
                List<String> names = getGame().getPlayers().stream().map(Player::getName).toList();
                List<Integer> scores = getGame().getPlayers().stream().map(Player::getScore).toList();
                return new ScoreBoardEvent(names, scores);
            }
        }
    }

    /**
     * Determines the next state based on the user's selection.
     * 
     * @return The next state
     */
    @Override
    public State nextState() {
        if (choice == 2) {
            return new MenuState(getGame());
        }

        if (choice == 1) {
            getGame().advanceDealer();
            return new BidState(this.getGame());
        }

        return this;
    }
}