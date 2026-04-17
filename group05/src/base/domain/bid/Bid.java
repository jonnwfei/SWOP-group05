package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;

import java.util.List;

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
     * Retrieves the playerId who holds this bid contract.
     *
     * @return The {@link Player} who made the bid.
     */
    PlayerId getPlayerId();

    /**
     * Determines the bidding team for this bid based on its specific rules.
     * Must be called immediately after the Bidding Phase,
     * before any cards are played, as some bids rely on inspecting full hands.
     *
     * @param allBids    All bids made this round (used to resolve dependent bids like Acceptance).
     * @param allPlayers All players in the round (used to find forced partners like in Troel).
     * @return A list of players forming the bidding team.
     * @throws IllegalStateException if the team cannot be determined (e.g., partner not found).
     */
    List<PlayerId> getTeam(List<Bid> allBids, List<Player> allPlayers);

    /**
     * Retrieves the specific type of the bid.
     *
     * @return The {@link BidType} enum value representing this bid.
     */
    BidType getType();

    /**
     * Calculates the base points earned or lost based on the target threshold.
     *
     * @param tricksWon The number of tricks successfully taken by the player/team.
     * @return The calculated score (positive for success, negative for failure).
     */
    int calculateBasePoints(int tricksWon);

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
}