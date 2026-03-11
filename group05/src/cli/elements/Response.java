package cli.elements;

import base.domain.actions.GameAction;

/**
 * Data wrapper for the communication between the UI and the Game Controller.
 * Encapsulates the user's validated action and the current state of the game loop.
 * * @author Stan Kestens
 * @since 08/03/2026
 */
public class Response {
    private final Boolean keepRunning;
    private final GameAction action;

    /**
     * @param keepRunning True if the game loop should continue processing.
     * @param action The validated action to be executed by the domain.
     */
    public Response(Boolean keepRunning, GameAction action){
        this.keepRunning = keepRunning;
        this.action = action;
    }

    /**
     * @return true if the UI should proceed to the next event.
     */
    public Boolean getContinue(){
        return this.keepRunning;
    }

    /**
     * @return The GameAction to be passed to the state machine.
     */
    public GameAction getAction(){
        return this.action;
    }
}