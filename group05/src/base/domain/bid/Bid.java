package base.domain.bid;

import base.domain.player.Player;
import base.domain.card.Suit;
import base.domain.trick.Trick;

import java.util.List;

/**
 * @author Seppe De Houwer, Tommy Wu
 * @since 24/2/26
 */
public interface Bid extends Comparable<Bid> {
    Player getPlayer();
    BidType getType();
    int calculateBasePoints(int tricksWon);
    Suit getChosenTrump(Suit dealtTrump);

    @Override
    default int compareTo(Bid other) {
        return this.getType().compareTo(other.getType());
    }


}