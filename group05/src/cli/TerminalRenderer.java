package cli;

import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.events.bidevents.BidTurnEvent;
import base.domain.events.bidevents.RejectedProposalEvent;
import base.domain.events.bidevents.SuitPromptEvent;
import base.domain.events.playevents.EndOfRoundEvent;
import base.domain.events.playevents.EndOfTrickEvent;
import base.domain.events.playevents.InitiateTurnEvent;
import base.domain.events.playevents.PickCardEvent;

import java.util.List;

public class TerminalRenderer {
    public void render(GameEvent event) {
        switch (event) {
            case BidTurnEvent e -> renderBidTurnEvent(e);
            case RejectedProposalEvent e -> renderRejectedProposalEvent(e);
            case SuitPromptEvent e -> renderSuitPromptEvent(e);
            case EndOfRoundEvent e -> renderEndOfRoundEvent(e);
            case EndOfTrickEvent e -> renderEndOfTrickEvent(e);
            case InitiateTurnEvent e -> renderInitiateTurnEvent(e);
            case PickCardEvent e -> renderPickCardEvent(e);
            case ErrorEvent e -> renderErrorEvent(e);
            default -> System.out.println("[WARNING] Unknown event type received!");        }
    }

    private void renderBidTurnEvent(BidTurnEvent event) {
        System.out.println("\n=== BIDDING TURN: " + event.playerName().toUpperCase() + " ===");
        System.out.println("Dealt Trump: " + event.dealtTrump().toString());
        if(event.currentHighestBidType() == null) {
            System.out.println("You are the first to bid!");
        }
        else System.out.println("Current Highest: " + event.currentHighestBidType());
        System.out.println("All Options:");
        for (int i = 0; i < event.bidTypes().length; i++) {
            System.out.println("   [" + i + "] " + event.bidTypes()[i].name());
        }
        System.out.print("Your choice: ");
    }

    private void renderRejectedProposalEvent(RejectedProposalEvent event) {
        System.out.println(event.proposerName() + ": no one accepted your proposal.");
        System.out.println("Choose [0] PASS or [1] SOLO_PROPOSAL:");
    }

    private void renderSuitPromptEvent(SuitPromptEvent event) {
        System.out.println("\n" + event.playerName() + ", you chose " + event.pendingType().name() + ".");
        System.out.println("All Options:");
        for (int i = 0; i < event.suits().length; i++) {
            System.out.println("   [" + i + "] " + event.suits()[i].name());
        }
        System.out.print("Your choice: ");
    }

    private void renderEndOfRoundEvent (EndOfRoundEvent event){
        System.out.println(event.playerName() + " played " + event.card().toString());
        System.out.println("\n============== ROUND OVER ==============");
        System.out.println("Calculating final scores...");
    }

    private void renderEndOfTrickEvent(EndOfTrickEvent event) {
        System.out.println(event.playerName() + " played " + event.card().toString());
        System.out.println("\n============== NEXT TRICK ==============");
        }

    private void renderInitiateTurnEvent (InitiateTurnEvent event) {
        System.out.println("============== Pass the turn to " + event.playerName() + " ==============");
        System.out.println("\nPress ANY BUTTON to reveal your hand...");
    }

    private void renderPickCardEvent(PickCardEvent event) {
        System.out.println("\n-------------- CARDS ON TABLE ---------------");

        if(event.cardsOnTable().isEmpty()) {
            System.out.println("(No cards played yet)");
        } else {
            for(String cardStr : event.cardsOnTable()) {
                System.out.println("- " + cardStr);
            }
        }

        // The 'if (false)' is replaced with our pure boolean data!
        if (event.isOpenMiserie()) {
            System.out.println("\n--- EXPOSED HAND (OPEN_MISERIE: " + event.exposedPlayerName() + ") ---");
            System.out.println(event.formattedExposedHand());
        }
        System.out.println("---------------------------------------------");

        // Fixed the math concatenation bug by putting the math in parentheses!
        System.out.println("Trick: " + event.trickNumber() + " | " + event.currentPlayerName() + "'s turn.");

        System.out.println("[0] Show last played Trick.");
        System.out.println("Your hand: ");

        // Fixed the <= out of bounds bug.
        // Notice we start formatting at [1] because [0] is reserved for "Show last trick"
        List<String> hand = event.currentPlayerHand();
        for (int i = 0; i < hand.size(); i++) {
            System.out.println("   [" + (i + 1) + "] " + hand.get(i));
        }

        System.out.print("Choose Card via index: ");
    }

    private void renderErrorEvent(ErrorEvent event) {
        System.out.println("Please give a number between " + event.lowerBound() + " and " + event.upperBound());
    }

}
