package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

/**
 * Represents an unresolved request for a partner.
 * MUST be resolved into AcceptedBid, SoloProposalBid or PassBid before the Playing Phase.
 */
public record ProposalBid(Player proposer) implements Bid {

    @Override
    public Player getPlayer() {return proposer;}

    @Override
    public BidType getType() {return BidType.PROPOSAL;}

    // --- DEFENSIVE PROGRAMMING BELOW ---
    // These methods belong to the Playing/Scoring phase.
    // If they are called, the engine has a bug.

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        throw new IllegalStateException("CRITICAL: An unresolved ProposalBid reached the play phase!");
    }

    @Override
    public int calculateBasePoints(int wonTricks) {
        throw new IllegalStateException("CRITICAL: Cannot score an unresolved ProposalBid!");
    }
}