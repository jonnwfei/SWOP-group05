package cli;

import base.domain.actions.ContinueAction;
import base.domain.actions.NumberAction;
import base.domain.actions.TextAction;
import base.domain.events.bidevents.BidTurnEvent;
import base.domain.events.GameEvent;
import cli.elements.Response;
import java.util.Scanner;

public class TerminalManager {

    private final Scanner scanner;
    private final TerminalRenderer renderer;

    /**
     *
     *
     */
    public TerminalManager() {
        this.scanner = new Scanner(System.in);
        this.renderer = new TerminalRenderer();
    }

    /**
     * Method is now PUBLIC so App can actually run IO tasks.
     */
    public Response handle(GameEvent event) {
        renderer.render(event);
        if (event.needsInput()){
            return getResponse();
        }
        return new Response(false, null);
    }

    /**
     * Reads the terminal and translates text into pure Domain data types.
     */
    private Response getResponse() {
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            return new Response(true, new ContinueAction());
        }

        try {
            // If the user typed a number, wrap it in a NumberAction
            int number = Integer.parseInt(input);
            return new Response(true, new NumberAction(number));
        } catch (NumberFormatException e) {
            // If they typed letters (like a name), wrap it in a TextAction
            return new Response(true, new TextAction(input));
        }
    }
}