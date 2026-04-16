package base.domain.states;

import base.domain.WhistGame;

import base.domain.commands.GameCommand;
import base.domain.commands.TextCommand;

import base.domain.results.GameResult;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import base.domain.player.Player;
import java.util.List;
import java.util.Optional;

import base.domain.commands.*;
import base.domain.results.*;
/**
 * Handles the end-of-round scoreboard display and provides options for where
 * they can go next.
 * 
 * @author John Cai
 * @since 09/03/2026
 */
public class ScoreBoardState extends State {

    private int choice = 0; // 0 = undecided, 1 = restart, 2 = quit
    private boolean awaitingSaveDescription = false;
    private final GamePersistenceService persistenceService = new GamePersistenceService();

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
     * @param command The user action
     * @return a GameEvent
     */
    @Override
    public GameResult executeState(Optional<GameCommand> command) {

        // If waiting for save description
        if (awaitingSaveDescription) {
            if (command.isPresent()) {
                return switch (command.get()) {
                    case TextCommand t -> {
                        try {
                            persistenceService.save(getGame(), SaveMode.GAME, t.text());
                            awaitingSaveDescription = false;
                            yield buildScoreBoard();
                        } catch (RuntimeException e) {
                            awaitingSaveDescription = false;
                            throw new IllegalStateException(
                                    "executeState in Scoreboard state failed to save", e
                            );
                        }
                    }
                    default -> new SaveDescriptionResult();
                };
            }
            return new SaveDescriptionResult();
        }

        // Normal flow
        if (command.isPresent()) {
            return switch (command.get()) {
                case NumberCommand n -> {
                    if (n.choice() == 1 || n.choice() == 2) {
                        this.choice = n.choice();
                        yield new ScoreBoardCompleteResult();
                    }
                    if (n.choice() == 3) {
                        awaitingSaveDescription = true;
                        yield new SaveDescriptionResult();
                    }
                    yield buildScoreBoard();
                }
                default -> buildScoreBoard();
            };
        }

        // No command → just render scoreboard
        return buildScoreBoard();
    }

    private GameResult buildScoreBoard() {
        List<String> names = getGame().getPlayers().stream().map(Player::getName).toList();
        List<Integer> scores = getGame().getPlayers().stream().map(Player::getScore).toList();
        return new ScoreBoardResult(names, scores);
    }

    /**
     * Determines the next state based on the user's selection.
     * 
     * @return The next state
     */
    @Override
    public State nextState() {
        if (choice == 2) {
            return null;
        }

        if (choice == 1) {
            getGame().advanceDealer();
            return new BidState(this.getGame());
        }

        return this;
    }
}