package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.commands.GameCommand;
import base.domain.results.GameResult;

import java.util.Optional;

/**
 * Common interface for all states.
 * 
 * @since 01/03
 * @author Stan Kestens
 */
public abstract class State {
    private WhistGame game;

    /**
     * @param game
     * @throws IllegalArgumentException if game is null
     */
    public State(WhistGame game) {
        if (game == null) throw new IllegalArgumentException("WhistGame cannot be null.");
        this.game = game;
    }
    public WhistGame getGame() {
        return this.game;
    }

    /**
     * Context passes itself through the state constructor. This may help a
     * state to fetch some useful context data if needed.
     */
    public abstract State nextState();
    public abstract GameResult executeState(Optional<GameCommand> action);
}