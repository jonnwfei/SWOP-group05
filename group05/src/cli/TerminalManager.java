package cli;

import cli.elements.BidTurnEvent;
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
    public Response handle(GameEvent event) {
        if (event instanceof BidTurnEvent bidEvent) {
            return renderBidTurnEvent(bidEvent);
        }
        // ... handle other events
        return null;
    }

        private Response renderBidTurnEvent(BidTurnEvent event) {
            System.out.println("\n=== BIDDING TURN: " + event.currentPlayer().getName().toUpperCase() + " ===");
            System.out.println("Dealt Trump: " + event.dealtTrump().toString());
            System.out.println("Current Highest: " + event.currentHighestBid());

            System.out.print("Your choice: ");

            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            return new Response(input, true);
        }
}