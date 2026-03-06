package base.domain.player;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;

import java.util.List;

/**
 * Represents a human-controlled participant in the game.
 * <p>
 * This strategy acts as the bridge between the core game engine and the external user interface (I/O).
 * Unlike automated bot strategies that calculate moves algorithmically, {@code HumanStrategy} relies
 * on external input to make decisions during the Bidding and Play phases. It is specifically flagged
 * to require confirmation before going to next round.
 *
 * @author Tommy Wu
 * @since 25/02/2026
 */
public class HumanStrategy implements Strategy {
    @Override
    public Bid determineBid(Player player) {
        return null;
    }

    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead) { // TODO:
        return null;
    }

    @Override
    public boolean requiresConfirmation() {
        return true;
    }
}
