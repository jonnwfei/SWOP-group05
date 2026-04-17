// base/domain/states/ScoreBoardState.java
package base.domain.states;

import base.domain.WhistGame;
import base.domain.commands.GameCommand;
import base.domain.commands.NumberCommand;
import base.domain.commands.TextCommand;
import base.domain.player.Player;
import base.domain.results.GameResult;
import base.domain.results.SaveDescriptionResult;
import base.domain.results.ScoreBoardResult;
import base.storage.snapshots.SaveMode;

import java.util.List;

/**
 * Handles the end-of-round scoreboard and the user's next-step choice.
 * <p>
 * IO-agnostic: emits {@link SaveDescriptionResult} to signal save intent and
 * reacts to the subsequent {@link TextCommand} (by which time the adapter has
 * already persisted the game) by clearing the flow and resuming the scoreboard.
 *
 * @author John Cai
 * @since 09/03/2026
 */
public class ScoreBoardState extends State {

    private int choice = 0; // 0 = undecided, 1 = restart, 2 = quit
    private boolean awaitingSaveDescription = false;

    public ScoreBoardState(WhistGame game) {
        super(game);
    }

    @Override
    public StateStep executeState() {
        if (awaitingSaveDescription) {
            return StateStep.stay(new SaveDescriptionResult(SaveMode.GAME));
        }
        return StateStep.stay(buildScoreBoard());
    }

    @Override
    public StateStep executeState(GameCommand command) {
        if (awaitingSaveDescription) {
            return switch (command) {
                // The adapter has already persisted the game by the time we see this.
                // We only need to clear the flag and go back to the scoreboard.
                case TextCommand ignored -> {
                    awaitingSaveDescription = false;
                    yield StateStep.stay(buildScoreBoard());
                }
                default -> StateStep.stay(new SaveDescriptionResult(SaveMode.GAME));
            };
        }

        return switch (command) {
            case NumberCommand n -> {
                if (n.choice() == 1 || n.choice() == 2) {
                    this.choice = n.choice();
                    yield StateStep.transitionWithoutResult();
                }
                if (n.choice() == 3) {
                    awaitingSaveDescription = true;
                    yield StateStep.stay(new SaveDescriptionResult(SaveMode.GAME));
                }
                yield StateStep.stay(buildScoreBoard());
            }
            default -> StateStep.stay(buildScoreBoard());
        };
    }

    private GameResult buildScoreBoard() {
        List<String> names = getGame().getPlayers().stream().map(Player::getName).toList();
        List<Integer> scores = getGame().getPlayers().stream().map(Player::getScore).toList();
        return new ScoreBoardResult(names, scores);
    }

    @Override
    public State nextState() {
        if (choice == 2) return null;
        if (choice == 1) {
            getGame().advanceDealer();
            return new BidState(this.getGame());
        }
        return this;
    }
}