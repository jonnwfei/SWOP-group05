package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.actions.TextAction;
import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.bid.Bid;
import base.domain.events.countEvents.ScoreBoardEvent;
import base.domain.events.playevents.PlayAgainPromptEvent;
import base.domain.events.playevents.ScoreBoardCompleteEvent;
import base.domain.player.Player;

import java.util.List;

/**
 * EndRoundState, mini menu state where the player can restart Round (12a)
 *
 * @author John Cai
 * @since 02/03/2026
 */

public class ScoreBoardState extends State {
    private final RestartTarget targetRestartTarget;
    private int choice = 0; // 0 = hasn't chosen, 1 = restart, 2 = quit

    public ScoreBoardState(WhistGame game, RestartTarget targetRestartTarget) {
        super(game);
        this.targetRestartTarget = targetRestartTarget;
    }
    public enum RestartTarget {
        BID_STATE,   // "Take me back to the bidding phase"
        COUNT_STATE  // "Take me back to the calculator phase"
    }
    @Override
    public GameEvent executeState(GameAction action) {
        // If we have a NumberAction, the user has already seen the scoreboard and answered
        if (action instanceof NumberAction numAction) {
            int input = numAction.value();
            if (input == 1 || input == 2) {
                this.choice = input;
                return new ScoreBoardCompleteEvent(); // Signal the end of this state
            }
            return new ErrorEvent(1, 2);
        }

        // If no action yet, show the ScoreBoard and the prompt
        List<String> names = getGame().getPlayers().stream().map(Player::getName).toList();
        List<Integer> scores = getGame().getPlayers().stream().map(Player::getScore).toList();
        return new ScoreBoardEvent(names, scores);
    }

    @Override
    public State nextState() {
        if (choice == 2) {
            return new MenuState(getGame());
        }

        if (choice == 1) {
            getGame().advanceDealer();

            // Use the instance variable 'targetRestartTarget'
            // compared against the Enum constant
            return (this.targetRestartTarget == RestartTarget.BID_STATE)
                    ? new BidState(getGame())
                    : new CountState(getGame());
        }

        return this;
    }
}