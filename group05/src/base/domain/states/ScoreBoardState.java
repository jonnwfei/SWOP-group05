package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.actions.TextAction;
import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.events.countEvents.ScoreBoardEvent;
import base.domain.events.menuEvents.AddPlayerEvent;
import base.domain.events.menuEvents.PrintNamesEvent;
import base.domain.events.menuEvents.RemovePlayerEvent;
import base.domain.events.menuEvents.RemoveRoundEvent;
import base.domain.events.playevents.ScoreBoardCompleteEvent;
import base.domain.player.HumanStrategy;
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

    private enum ScoreBoardPhase {
        SHOW_SCORES,
        ADD_PLAYER,
        REMOVE_PLAYER,
        REMOVE_ROUND
    }

    private ScoreBoardPhase phase = ScoreBoardPhase.SHOW_SCORES;
    private int choice = 0; // 0 = undecided, 1 = restart, 2 = quit, 3 = add player, 4 = remove player, 5 = remove round

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
        switch (phase) {
            case SHOW_SCORES -> {
                return handleScoreBoardChoice(action);
            }
            case ADD_PLAYER -> {
                return handleAddPlayer(action);
            }
            case REMOVE_PLAYER -> {
                return handleRemovePlayer(action);
            }
            case REMOVE_ROUND -> {
                return handleRemoveRound(action);
            }
        }
        return new ErrorEvent(1, 5);
    }

    private GameEvent<?> handleScoreBoardChoice(GameAction action) {
        switch (action) {
            case NumberAction(int input) -> {
                choice = input;
                if (choice >= 1 && choice <= 5) {
                    if (choice == 3) {
                        phase = ScoreBoardPhase.ADD_PLAYER;
                        return new AddPlayerEvent();
                    } else if (choice == 4) {
                        phase = ScoreBoardPhase.REMOVE_PLAYER;
                        return new RemovePlayerEvent(getGame().getAllPlayers());
                    } else if (choice == 5) {
                        phase = ScoreBoardPhase.REMOVE_ROUND;
                        return new RemoveRoundEvent(getGame().getRounds());
                    }
                    return new ScoreBoardCompleteEvent();
                }
                return new ErrorEvent(1, 5);
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
     * Handles adding a new player to the game.
     *
     * @param action The user input containing the player name.
     * @return GameEvent
     */
    private GameEvent<?> handleAddPlayer(GameAction action) {
        String name = switch (action) {
            case TextAction(String playerName) -> playerName;
            default -> null;
        };

        if (name == null || name.trim().isEmpty()) {
            return new AddPlayerEvent();
        }

        getGame().addPlayer(new Player(new HumanStrategy(), name));
        phase = ScoreBoardPhase.SHOW_SCORES;
        choice = 0;
        List<String> names = getGame().getPlayers().stream().map(Player::getName).toList();
        List<Integer> scores = getGame().getPlayers().stream().map(Player::getScore).toList();
        return new ScoreBoardEvent(names, scores);
    }

    /**
     * Handles the removal of a player based on user-selected index.
     *
     * @param action The user input containing the index.
     * @return GameEvent
     */
    private GameEvent<?> handleRemovePlayer(GameAction action) {
        int index = switch (action) {
            case NumberAction(int selected) -> selected;
            default -> -1;
        };

        if (index == -1 || index < 0 || index >= getGame().getAllPlayers().size()) {
            return new RemovePlayerEvent(getGame().getAllPlayers());
        }

        getGame().removePlayer(getGame().getAllPlayers().get(index));
        phase = ScoreBoardPhase.SHOW_SCORES;
        choice = 0;
        List<String> names = getGame().getPlayers().stream().map(Player::getName).toList();
        List<Integer> scores = getGame().getPlayers().stream().map(Player::getScore).toList();
        return new ScoreBoardEvent(names, scores);
    }

    /**
     * Handles the removal of a round based on user-selected index.
     *
     * @param action The user input containing the index.
     * @return GameEvent
     */
    private GameEvent<?> handleRemoveRound(GameAction action) {
        int index = switch (action) {
            case NumberAction(int selected) -> selected;
            default -> -1;
        };

        if (index == -1 || index < 0 || index >= getGame().getRounds().size()) {
            return new RemoveRoundEvent(getGame().getRounds());
        }

        getGame().removeRound(getGame().getRounds().get(index));
        phase = ScoreBoardPhase.SHOW_SCORES;
        choice = 0;
        List<String> names = getGame().getPlayers().stream().map(Player::getName).toList();
        List<Integer> scores = getGame().getPlayers().stream().map(Player::getScore).toList();
        return new ScoreBoardEvent(names, scores);
    }

    /**
     * Utility to extract current player names.
     */
    private List<String> getPlayerNames() {
        return getGame().getAllPlayers().stream().map(Player::getName).toList();
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
            return new BidState(this.getGame());
        }
        phase = ScoreBoardPhase.SHOW_SCORES;
        return this;
    }
}