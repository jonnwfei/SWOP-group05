package base.domain.events.playevents;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.trick.Turn;
import base.domain.events.GameEvent;

import java.util.List;

public record PickCardEvent(Round round, Player player, List<Card> hand) implements GameEvent {
    private String renderPickCardEvent(){
        return buildTableDisplay() + "\nTrick: " + (round.getTricks().size() + 1) +
                " | " + player.getName() + "'s turn.\n" + "(0) to show last played Trick.\n" +
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
