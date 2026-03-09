package cli.elements;

import base.domain.card.Suit;
import base.domain.player.Player;

public record FirstBidTurnEvent(Player currentPlayer, Suit dealtTrump) implements GameEvent{

    private void renderBidTurnEvent(BidTurnEvent event) {
        System.out.println("\n=== BIDDING TURN: " + event.currentPlayer().getName().toUpperCase() + " ===");
        System.out.println("Dealt Trump: " + event.dealtTrump().toString());
        System.out.println("Your Hand:" + event.currentPlayer().getFormattedHand());
        System.out.println("You are the first to bid!");
        System.out.println("---------------------------------------");
        System.out.print("Your choice: ");
    }

}
