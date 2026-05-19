package base.domain.bid;

import base.domain.card.Suit;

/**
 * The core contract for all bids made during the bidding phase.
 * Defines how a bid is ranked, who made it, its trump suit, and how it is scored.
 *
 * @author Seppe De Houwer, Tommy Wu
 * @since 24/02/26
 */
public sealed interface Bid extends Comparable<Bid> permits
        PassBid,
        ProposalBid,
        SoloProposalBid,
        AcceptedBid,
        AbondanceBid,
        MiserieBid,
        SoloBid,
        TroelBid
{

    /**
     * Retrieves the specific type of the bid.
     *
     * @return The {@link BidType} enum value representing this bid.
     */
    BidType getType();

    /**
     * Determines the active trump suit for the round if this is the highest bid at the end of bidding phase.
     *
     * @param dealtTrump The original trump suit dealt at the start of the round.
     * @return The chosen Trump {@link Suit} to be used as trump.
     */
    Suit determineTrump(Suit dealtTrump);

    /**
     * Compares this bid to another to determine which is higher in the bidding phase.
     * Relies on the natural ordering defined within the {@link BidType} enum.
     *
     * @param other The competing {@link Bid} to compare against.
     * @return A negative integer, zero, or a positive integer as this bid
     * is less than, equal to, or greater than the specified bid.
     */
    @Override
    default int compareTo(Bid other) {
        return this.getType().compareTo(other.getType());
    }

    int teamSize();
}