package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.actions.TextAction;
import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.bid.Bid;
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
    public enum RestartTarget {
        BID_STATE, COUNT_STATE
    }

    private boolean userWantsToQuit = false;
    private boolean userWantsToRestart = false;
    private final RestartTarget targetRestartTarget; // Can hold either new BidState or CountState

    /**
     * Constructs a ScoreBordState for prompting the player whether to restart or quit to the menu. (Can be used for either
     * PlayState and CountState, avoiding double code)
     * @param game that will hold this state
     * @param targetRestartTarget can either be new CountState or new BidState
     */
    public ScoreBoardState(WhistGame game, RestartTarget targetRestartTarget) {
        super(game);
        this.targetRestartTarget = targetRestartTarget;
    }

    /**
     *
     * @param action
     * @return GameEvent either TextEvent which calls nextState, or QuestionEvent which returns the currentState
     */
    @Override
    public GameEvent executeState(GameAction action) {
        // 1. Process User Input
        if (action instanceof NumberAction numAction) {
            int choice = numAction.value();

            if (choice == 1) {
                userWantsToRestart = true;
                return new ScoreBoardCompleteEvent();
            } else if (choice == 2) {
                userWantsToQuit = true;
                return new ScoreBoardCompleteEvent();
            } else {
                return new ErrorEvent(1, 2);
            }
        }

        // 2. Catch invalid typing
        if (action instanceof TextAction) {
            return new ErrorEvent(1, 2);
        }

        List<String> playerNames = getGame().getPlayers().stream().map(Player::getName).toList();
        List<Integer> playerScores = getGame().getPlayers().stream().map(Player::getScore).toList();
        return new PlayAgainPromptEvent(playerNames, playerScores);
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
            getGame().advanceDealer();

            if (targetRestartTarget == RestartTarget.BID_STATE) {
                return new BidState(getGame());
            }
            else {
                return new CountState(getGame());
            }
        } else {
            return this;
        }
    }
}
