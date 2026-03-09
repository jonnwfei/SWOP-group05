package cli;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.events.GameEvent;
import base.domain.events.bidevents.BidTurnEvent;
import base.domain.events.bidevents.RejectedProposalEvent;
import base.domain.events.bidevents.SuitPromptEvent;
import base.domain.player.Player;
import base.domain.trick.Turn;
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
        }
    }

    private void renderBidTurnEvent(BidTurnEvent event) {
        System.out.println("\n=== BIDDING TURN: " + event.currentPlayer().getName().toUpperCase() + " ===");
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
        System.out.println(event.proposer().getName() + ": no one accepted your proposal.");
        System.out.println("Choose [0] PASS or [1] SOLO_PROPOSAL:");
    }

    private void renderSuitPromptEvent(SuitPromptEvent event) {
        System.out.println("\n" + event.currentPlayer().getName() + ", you chose " + event.pendingType().name() + ".");
        System.out.println("All Options:");
        for (int i = 0; i < event.suits().length; i++) {
            System.out.println("   [" + i + "] " + event.suits()[i].name());
        }
        System.out.print("Your choice: ");
    }

    private void renderEndOfRoundEvent (EndOfRoundEvent event){
        System.out.println(event.player().getName() + "played " + event.card().toString());
        System.out.println("\n============== ROUND OVER ==============");
        System.out.println("Calculating final scores...");
    }

    private void renderEndOfTrickEvent(EndOfTrickEvent event) {
        System.out.println(event.player().getName() + "played " + event.card().toString());
        System.out.println("\n============== NEXT TRICK ==============");
        }

    private void renderInitiateTurnEvent (InitiateTurnEvent event) {
        System.out.println("============== Pass the turn to " + event.player().getName() + " ==============");
        System.out.println("\nPress ANY BUTTON to reveal your hand...");
    }

    private void renderPickCardEvent(PickCardEvent event){
        System.out.println("Trick: " + event.round().getTricks().size() + 1 + " | " + event.player().getName() + "'s turn.");
        System.out.println("(0) to show last played Trick.");
        System.out.println("Your hand: ");
        for (int i = 0; i <= event.hand().size(); i++) {
            System.out.println("[" + i + "] " + event.hand().get(i).toString());
        }
        System.out.println("Your hand: ");

        return buildTableDisplay() + "\nTrick: " + (.getTricks().size() + 1) +
                " | " + player.getName() + "'s turn.\n" + "[0] to show last played Trick.\n" +
                "Your hand: \n" + getFormattedHand(false) + "\nChoose Card via index:";
    }


    /**
     * Returns a formatted, 1-indexed string of the player's current hand.
     * <br>
     * Example: "(1) ACE of HEARTS \n (2) TEN of HEARTS"
     * @return a formatted string of currentHand
     */
    public String getFormattedHand(boolean showIdx) {
        StringBuilder sb = new StringBuilder();
        List<Card> hand = player.getHand();

        for (int i = 0; i < hand.size(); i++) {
            // i + 1 ensures the terminal list starts at 1 instead of 0
            if (showIdx) sb.append("(").append(i + 1).append(") ").append(hand.get(i).toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Builds a string containing the currently Played cards and if applicable, the Hand of the player playing OPEN MISERIE
     *
     * @return formatted string that holds the current table status of open cards
     */
    private String buildTableDisplay() {
        StringBuilder table = new StringBuilder("\n-------------- CARDS ON TABLE ---------------\n");
        if (round.getTricks().getLast().getTurns().isEmpty()) {
            table.append("(No cards played yet)\n");
        } else {
            for (Turn turn : round.getTricks().getLast().getTurns()) {
                table.append("- ").append(turn.toString()).append("\n");
            }
        }

        Bid highestBid = round.getHighestBid();
        if (highestBid != null && highestBid.getType() == BidType.OPEN_MISERIE) {
            Player exposedPlayer = highestBid.getPlayer();
            table.append("\n--- EXPOSED HAND (OPEN_MISERIE: ").append(exposedPlayer.getName()).append(")---\n");
            table.append(getFormattedHand(false)).append("\n");
        }
        table.append("---------------------------------------------\n");
        return table.toString();
    }
}
