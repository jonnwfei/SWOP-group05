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
            renderBidTurnEvent(bidEvent);
            return getResponse();
        }
        // ... handle other events
        return null;
    }

        private void renderBidTurnEvent(BidTurnEvent event) {
            System.out.println("\n=== BIDDING TURN: " + event.currentPlayer().getName().toUpperCase() + " ===");
            System.out.println("Dealt Trump: " + event.dealtTrump().toString());
            if(event.currentHighestBidType() == null) {
                System.out.println("You are the first to bid!");
            }
            System.out.println("Current Highest: " + event.currentHighestBidType());

            System.out.print("Your choice: ");
        }

        private Response getResponse() {
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            return new Response(input, true);
        }
}