package base.domain.player;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;
import java.util.List;

/**
 * Represents a human-controlled participant in the game.
 * <p>
 * This strategy acts as a placeholder that signals the game engine to wait for
 * external user input. Unlike automated bots, {@code HumanStrategy} does not
 * calculate moves algorithmically; instead, the {@code GameController} and
 * active {@code State} collect input and apply it to the player's hand.
 * </p>
 *
 * @author Tommy Wu
 * @since 25/02/2026
 */
public class HumanStrategy implements Strategy {

    /**
     * Returns null as the bid is determined via external UI input
     * captured by the bidState
     * @param player The player instance.
     * @return null.
     */
    @Override
    public Bid determineBid(Player player) {
        return null;
    }

    /**
     * Returns null as the card selection is handled via external UI input
     * captured by the {@code PlayState}.
     * @param currentHand The player's current hand.
     * @param lead The suit led in the current trick.
     * @return null.
     */
    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead) {
        return null;
    }

    /**
     * Signals that this strategy requires manual intervention.
     * @return true, ensuring the UI flow prompts the user before proceeding.
     */
    @Override
    public boolean requiresConfirmation() {
        return true;
    }
}