package cli.elements;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;

public record SuitPromptEvent(Player currentPlayer, BidType pendingType) implements GameEvent {
    private void RenderSuitPromptEvent(SuitPromptEvent event) {
        System.out.println("\n" + event.currentPlayer().getName() + ", you chose " + event.pendingType().name() + ".");
        System.out.print(buildOptions(Suit.values()));
        System.out.print("Your choice: ");
    }
}
