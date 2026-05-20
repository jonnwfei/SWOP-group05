package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.TeamRole;
import base.domain.snapshots.StrategySnapshotType;

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
public final class HumanStrategy implements Strategy {

    /**
     * Returns null as the bid is determined via external UI input
     * captured by the bidState
     * @return null.
     */
    @Override
    public Bid determineBid(List<Card> hand) {
        return null;
    }

    /**
     * Returns null as the card selection is handled via external UI input
     * captured by the {@code PlayState}.
     *
     * @param currentHand The player's current hand.
     * @param lead        The suit led in the current trick.
     * @param role
     * @return null.
     */
    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead, TeamRole role) {
        return null;
    }

    /**
     * Returns the snapshot type for this strategy, used for serialization and game state representation.
     * @return the enum value for HUMAN
     */
    public StrategySnapshotType toSnapshotType() {
        return StrategySnapshotType.HUMAN;
    }
}