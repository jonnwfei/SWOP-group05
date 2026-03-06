package cli;

import cli.elements.GameEvent;
import cli.elements.Response;
import java.util.Scanner;

public class TerminalManager {

    private final Scanner scanner;

    /**
     *
     *
     */
    public TerminalManager() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Method is now PUBLIC so App can actually run IO tasks.
     */
    public Response handle(GameEvent element) {
        if (element.isInputRequired()) {
            System.out.print(element.getContent() + " ");
            String text = scanner.nextLine();
            return new Response(text, true);
        } else {
            System.out.println(element.getContent());
            return new Response(null, false);
        }
    }
}