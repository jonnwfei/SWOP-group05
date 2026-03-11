package cli;

import base.domain.actions.ContinueAction;
import base.domain.actions.NumberAction;
import base.domain.actions.NumberListAction;
import base.domain.actions.TextAction;
import base.domain.events.GameEvent;
import base.domain.events.errorEvents.NumberErrorEvent;
import base.domain.events.errorEvents.NumberListErrorEvent;
import cli.elements.Response;

import java.util.ArrayList;
import java.util.Scanner;

public class TerminalManager {

    private final Scanner scanner;
    private final TerminalRenderer renderer;
    private final TerminalParser parser;
    /**
     *
     *
     */
    public TerminalManager() {
        this.scanner = new Scanner(System.in);
        this.renderer = new TerminalRenderer();
        this.parser = new TerminalParser();
    }

    /**
     * Method is now PUBLIC so App can actually run IO tasks.
     */
    public Response handle(GameEvent<?> event) {
        try {
            renderer.render(event);
            if (event.needsInput()) {
                return getResponse(event);
            }
            return new Response(false, null);
        } catch (IllegalStateException e) {

            if (event.getInputType() == Integer.class) {
                GameEvent<Integer> intEvent = (GameEvent<Integer>) event;
                NumberErrorEvent errorEvent = new NumberErrorEvent(e.getMessage(), intEvent::isValid);
                return handle(errorEvent);
            }
            if(event.getInputType() == String.class){
                return handle(event);
            }
            GameEvent<ArrayList<Integer>> listEvent = (GameEvent<ArrayList<Integer>>) event;
            return handle(new NumberListErrorEvent(listEvent::isValid));
        }
    }

    /**
     * Reads the terminal and translates text into pure Domain data types.
     */
    private Response getResponse(GameEvent<?> event) {
        String rawInput = scanner.nextLine().trim();
        try {
            // 1. Handle "Press Enter to Continue" (No parsing needed!)
            if (event.getInputType() == Void.class) {
                return new Response(true, new ContinueAction());
            }

            // 2. Handle String Inputs
            if (event.getInputType() == String.class) {
                String parsed = parser.parseString(rawInput);
                validateOrThrow(event, parsed);
                return new Response(true, new TextAction(parsed));
            }

            // 3. Handle Integer Inputs
            else if (event.getInputType() == Integer.class) {
                Integer parsed = parser.parseNumberInput(rawInput);
                validateOrThrow(event, parsed);
                return new Response(true, new NumberAction(parsed));
            }

            // 4. Handle List Inputs (ArrayList<Integer>)
            else if (event.getInputType().equals(ArrayList.class) ||
                    event.getInputType().getName().contains("ArrayList")) {
                ArrayList<Integer> parsed = parser.parseNumbersInput(rawInput);
                validateOrThrow(event, parsed);
                return new Response(true, new NumberListAction(parsed));
            }

            throw new IllegalStateException("Unsupported input type: " + event.getInputType());

        } catch (IllegalArgumentException e) {
            // If the parser fails or validation fails, wrap it in IllegalStateException
            throw new IllegalStateException("Invalid input: " + e.getMessage());
        }
    }

    /**
     * Helper to bridge your boolean isValid check with the Exception-based flow.
     */
    private <T> void validateOrThrow(GameEvent<T> event, Object parsed) {
        @SuppressWarnings("unchecked")
        T typedInput = (T) parsed;
        if (!event.isValid(typedInput)) {
            throw new IllegalArgumentException("Input does not meet the requirements");
        }
    }
}