package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

/**
 * Represents a player's initial offer to form a partnership.
 * Acts as a transitional contract that must be resolved (accepted, passed,
 * or upgraded to a Solo Proposal) before the Playing Phase begins.
 */
public record ProposalBid(Player proposer) implements Bid {

    public ProposalBid {
        if (proposer == null) {throw new IllegalArgumentException("Proposer cannot be null.");}
    }

    @Override
    public Player getPlayer() {return proposer;}

    @Override
    public List<Player> getTeam(List<Bid> allBids, List<Player> allPlayers) {
        int totalCards = allPlayers.stream().mapToInt(p -> p.getHand().size()).sum();
        if (totalCards != 52) {
            throw new IllegalStateException("getTeam() can only be called before the play phase begins!");
        }
        Player acceptor = allBids.stream().filter(bid -> bid.getType() == BidType.ACCEPTANCE).map(Bid::getPlayer).findFirst().orElse(null);
        if (acceptor == null) {throw new IllegalArgumentException("There was no acceptor found in allBids, it's impossible to have ProposalBid without AcceptedBid!");}
        return List.of(proposer, acceptor);
    }

    @Override
    public BidType getType() {return BidType.PROPOSAL;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        if (dealtTrump == null) {throw new IllegalArgumentException("Dealt trump suit cannot be null.");}
        return dealtTrump;
    }

    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0 || tricksWon > 13) {throw new IllegalArgumentException("tricks won is out of bound, min 0 max 13");}
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