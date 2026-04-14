package base.domain.bid;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static base.domain.card.CardMath.getHighestRankOfSuit;

/**
 * Represents a forced contract triggered when a player holds 3 or 4 Aces.
 * The player forms a team with a forced partner (the holder of the 4th Ace, or the highest Heart).
 *
 * @param player  The player forced into the bid (holding the Aces).
 * @param bidType The specific Troel rank (TROEL for 3 Aces, TROELA for 4 Aces).
 * @author Tommy Wu
 * @since 01/04/2026
 */
public record TroelBid(Player player, BidType bidType) implements Bid {

    public TroelBid {
        if (player == null) {throw new IllegalArgumentException("player can't be null");}
        if (bidType == null) {throw new IllegalArgumentException("bidType can't be null");}
        if (bidType.getCategory() != BidCategory.TROEL) {throw new IllegalArgumentException("TroelBid requires a TROEL category!");}

        long aceCount = player.getHand().stream()
                .filter(card -> card.rank() == Rank.ACE)
                .count();

        if (aceCount < 3) {throw new IllegalArgumentException("Player doesn't meet conditions");}
        if (aceCount == 4 && bidType == BidType.TROEL) {throw new IllegalArgumentException("wrong bidType given");}
        if (aceCount == 3 && bidType == BidType.TROELA) {throw new IllegalArgumentException("wrong bidType given");}
    }

    @Override
    public Player getPlayer() {return player;}

    /**
     * Determines the attacking team by finding the forced partner.
     * For TROEL: The partner is the player with the 4th (missing) Ace.
     * For TROELA: The partner is the player with the highest card in Hearts.
     *
     * @param allBids    All bids placed during the round.
     * @param allPlayers All players in the game.
     * @return A list containing the Troel bidder and their forced partner.
     * @throws IllegalStateException if the forced partner cannot be found (e.g., the deck is corrupted or missing cards).
     */
    @Override
    public List<Player> getTeam(List<Bid> allBids, List<Player> allPlayers) {
        Player partner;

        if (bidType == BidType.TROEL) {
            // Find the player with the missing Ace
            Card missingAce = new Card(this.determineTrump(null), Rank.ACE);
            partner = allPlayers.stream()
                    .filter(p -> p.hasCard(missingAce))
                    .findFirst()
                    .orElse(null);
        } else {
            // Find the player with the highest card in the suit of Hearts (TROELA)
            partner = allPlayers.stream()
                    .filter(p -> !p.equals(player))
                    .max(Comparator.comparing(p -> getHighestRankOfSuit(Suit.HEARTS, p.getHand()), Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(null);
        }

        if (partner == null) {
            throw new IllegalStateException("Partner not found. The deck might be corrupted or mis-dealt!");
        }

        return List.of(player, partner);
    }

    @Override
    public BidType getType() {return bidType;}

    /**
     * Calculates the points won or lost based on tricks taken.
     * Earns extra points (+2) for each overtrick, and doubles the total score if all 13 tricks are won.
     *
     * @param tricksWon The combined number of tricks won by the team.
     * @return Positive calculated points if the contract was met, negative base points if failed.
     */
    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        int points = bidType.getBasePoints();
        int extra = tricksWon - bidType.getTargetTricks();
        if (extra < 0) {
            points = -1 * points;
            return points;
        }
        points += 2*extra;
        if (tricksWon == 13) {points = 2*points;}
        return points;
    }

    /**
     * Automatically determines the trump suit for the Troel contract.
     * For TROEL: The trump suit is the suit of the missing 4th Ace.
     * For TROELA: The trump suit is forced to Hearts.
     *
     * @param dealtTrump The default trump suit dealt at the start (ignored for Troel).
     * @return The calculated trump suit.
     * @throws IllegalStateException if the missing Ace cannot be mathematically determined.
     */
    @Override
    public Suit determineTrump(Suit dealtTrump) {
        List<Suit> aces = getAces();
        if (aces.size() == 4) {
            return Suit.HEARTS;
        }
        return Arrays.stream(Suit.values())
                .filter(suit -> !aces.contains(suit))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing 4th ace not found"));    }

    /**
     * Helper method to extract the suits of all Aces currently in the player's hand.
     * @return A list of suits corresponding to the Aces held.
     */
    private List<Suit> getAces() {
        return player.getHand().stream().filter(card -> card.rank() == Rank.ACE).map(Card::suit).toList();
    }

}
