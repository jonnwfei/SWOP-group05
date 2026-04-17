package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;

import java.util.List;

/**
 * Represents a contract where a player accepts another player's Proposal.
 * @param acceptor The player who agreed to the proposal.
 * @author Tommy Wu
 * @since 25/02/2026
 */
public record AcceptedBid(PlayerId acceptor) implements Bid {

    public AcceptedBid {
        if (acceptor == null) {throw new IllegalArgumentException("Acceptor cannot be null.");}
    }

    @Override
    public PlayerId getPlayerId() {return acceptor;}

    /**
     * Determines the bidding team by pairing the Acceptor with the original Proposer.
     *
     * @param allBids    All bids placed during the round to search for the Proposal.
     * @param allPlayers All players in the game.
     * @return A list containing both the Acceptor and the Proposer.
     * @throws IllegalArgumentException if no Proposal bid is found in the bid history.
     */
    @Override
    public List<PlayerId> getTeam(List<Bid> allBids, List<Player> allPlayers) {
        PlayerId proposer = allBids.stream().filter(bid -> bid.getType() == BidType.PROPOSAL).map(Bid::getPlayerId).findFirst().orElse(null);
        if  (proposer == null) {throw new IllegalArgumentException("There was no proposer found in allBids, it's impossible to have AcceptedBid without ProposalBid!");}
        return List.of(acceptor, proposer);
    }

    @Override
    public BidType getType() {return BidType.ACCEPTANCE;}

    /**
     * An Accepted Proposal is always played using the originally dealt trump suit.
     *
     * @param dealtTrump The default trump suit dealt at the start.
     * @return The dealt trump suit.
     * @throws IllegalArgumentException if dealtTrump is null.
     */
    @Override
    public Suit determineTrump(Suit dealtTrump) {
        if (dealtTrump == null) {throw new IllegalArgumentException("Dealt trump suit cannot be null.");}
        return dealtTrump;
    }

    /**
     * Calculates the points won or lost based on tricks taken.
     * Earns extra points for overtricks, and doubles the score if all 13 tricks are won.
     *
     * @param tricksWon The combined number of tricks won by the team.
     * @return Positive calculated points if the contract was met, negative base points if failed.
     */
    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        int points = BidType.ACCEPTANCE.getBasePoints();

        int extra = tricksWon - BidType.ACCEPTANCE.getTargetTricks();
        if (extra < 0) {
            points = -1 * points;
            return points;
        }
        points += extra;
        if (tricksWon == 13) {points = 2*points;}
        return points;
    }
}
