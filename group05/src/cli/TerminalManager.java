package cli;

import cli.elements.Response;
import cli.events.IOEvent;

import java.util.Scanner;

public class TerminalManager {

    private final Scanner scanner;
    private final TerminalRenderer renderer;

    public TerminalManager() {
        this.scanner = new Scanner(System.in);
        this.renderer = new TerminalRenderer();
    }

    public Response handle(IOEvent event) {
        renderer.render(event);
        if (event.needsInput()) {
            return new Response(scanner.nextLine().trim());
        }
        return new Response(null);
    }
}