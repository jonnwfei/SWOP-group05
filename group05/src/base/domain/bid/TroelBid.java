package base.domain.bid;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;

import java.util.Comparator;
import java.util.List;

import static base.domain.card.CardMath.getHighestRankOfSuit;

/**
 * Represents a forced contract triggered when a player holds 3 or 4 Aces.
 * The player forms a team with a forced partner (the holder of the 4th Ace, or the highest Heart).
 *
 * @param playerId  The ID of the player forced into the bid.
 * @param bidType   The specific Troel rank (TROEL or TROELA).
 * @param trumpSuit The suit of the missing 4th Ace (for TROEL). Ignored for TROELA.
 * @author Tommy Wu
 * @since 01/04/2026
 */
public record TroelBid(PlayerId playerId, BidType bidType, Suit trumpSuit) implements Bid {

    public TroelBid(PlayerId playerId, BidType bidType, Suit trumpSuit) {
        if (playerId == null) throw new IllegalArgumentException("PlayerId can't be null");
        if (bidType == null) throw new IllegalArgumentException("BidType can't be null");
        if (bidType.getCategory() != BidCategory.TROEL) throw new IllegalArgumentException("TroelBid requires a TROEL category!");

        this.playerId = playerId;
        this.bidType = bidType;

        // Rule Enforcement: If it's TROELA, the trump is ALWAYS Hearts.
        if (bidType == BidType.TROELA) {
            this.trumpSuit = Suit.HEARTS;
        } else {
            // For standard TROEL, the caller MUST provide the suit of the missing Ace.
            if (trumpSuit == null) throw new IllegalArgumentException("TROEL requires the suit of the missing Ace.");
            this.trumpSuit = trumpSuit;
        }
    }

    @Override
    public PlayerId getPlayerId() {return playerId;}

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
    public List<PlayerId> getTeam(List<Bid> allBids, List<Player> allPlayers) {
        PlayerId partnerId;

        if (bidType == BidType.TROEL) {
            Card missingAce = new Card(this.trumpSuit, Rank.ACE);
            partnerId = allPlayers.stream()
                    .filter(p -> p.hasCard(missingAce))
                    .map(Player::getId) // Return ONLY the ID
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Partner not found. Corrupted deck!"));
        } else {
            // TROELA: Partner is the player with the highest Heart (excluding the bidder)
            partnerId = allPlayers.stream()
                    .filter(p -> !p.getId().equals(playerId))
                    .max(Comparator.comparing(p -> getHighestRankOfSuit(Suit.HEARTS, p.getHand()), Comparator.nullsFirst(Comparator.naturalOrder())))
                    .map(Player::getId)
                    .orElseThrow(() -> new IllegalStateException("Partner not found. Corrupted deck!"));
        }

        return List.of(playerId, partnerId);
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

    @Override
    public Suit determineTrump(Suit dealtTrump) {
        return this.trumpSuit;
    }
}
