package base.domain.bid;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static base.domain.card.CardMath.getHighestRankOfSuit;

/**
 * Owns the PlayerId <-> Bid mapping and all bid-history reasoning for a single Round.
 * Acts as the link between Round, Player and Bid: Bid stays player-agnostic,
 * Round and BidState ask the manager who placed/accepted/partnered which bid.
 *
 * Lifecycle: one BidManager per Round. Constructed by Round, shared with BidState.
 */
public final class BidManager {

    /** The 4 players in the round, in seating order. Held only for Troel ace lookups. */
    private final List<Player> players;

    /** PlayerId -> their committed Bid for this round. Insertion order = placement order. */
    private final LinkedHashMap<PlayerId, Bid> bidsByPlayer = new LinkedHashMap<>();

    /** The currently highest committed bid, or null if no non-PASS bid has been placed. */
    private Bid highestBid;

    /** PlayerId of the player who placed {@link #highestBid}, or null. */
    private PlayerId highestBidder;

    public BidManager(List<Player> players) {
        if (players == null || players.size() != 4)
            throw new IllegalArgumentException("BidManager requires exactly 4 players.");
        this.players = List.copyOf(players);
    }

    // ---------------------------------------------------------------------
    // Placement
    // ---------------------------------------------------------------------

    /**
     * Instantiates and registers a bid for the given player.
     * Updates the highest-bid tracking if the new bid outranks the current one.
     *
     * @param playerId  the player placing the bid
     * @param bidType   the chosen bid type (must be legal for this player)
     * @param trumpSuit chosen trump (only used for ABONDANCE/SOLO with chosen-suit) or
     *                  the missing-Ace suit for TROEL; null otherwise
     * @return the newly created Bid
     */
    public Bid placeBid(PlayerId playerId, BidType bidType, Suit trumpSuit) {
        if (playerId == null) throw new IllegalArgumentException("playerId required");
        if (bidType == null) throw new IllegalArgumentException("bidType required");

        Bid bid = bidType.instantiate(trumpSuit);  // TRANSITIONAL: until BidType.instantiate drops PlayerId
        bidsByPlayer.put(playerId, bid);

        if (bidType != BidType.PASS && (highestBid == null || bid.compareTo(highestBid) > 0)) {
            highestBid = bid;
            highestBidder = playerId;
        }
        return bid;
    }

    /**
     * Drops the outstanding PROPOSAL (used when no acceptance arrives or it is rejected).
     * Replaces the proposer's bid with PASS and recomputes the highest bid.
     */
    public void invalidateProposal() {
        PlayerId proposer = findProposer().orElse(null);
        if (proposer == null) return;
        bidsByPlayer.put(proposer, BidType.PASS.instantiate(null));
        recomputeHighest();
    }

    private void recomputeHighest() {
        highestBid = null;
        highestBidder = null;
        for (Map.Entry<PlayerId, Bid> e : bidsByPlayer.entrySet()) {
            Bid b = e.getValue();
            if (b.getType() == BidType.PASS) continue;
            if (highestBid == null || b.compareTo(highestBid) > 0) {
                highestBid = b;
                highestBidder = e.getKey();
            }
        }
    }

    // ---------------------------------------------------------------------
    // Queries: highest bid, completion, legality
    // ---------------------------------------------------------------------

    public Optional<Bid> getHighestBid() { return Optional.ofNullable(highestBid); }
    public Optional<PlayerId> getHighestBidder() { return Optional.ofNullable(highestBidder); }

    /** Reverse lookup: which player placed the given Bid (object identity)? */
    public PlayerId getBidderOf(Bid bid) {
        for (Map.Entry<PlayerId, Bid> e : bidsByPlayer.entrySet()) {
            if (e.getValue() == bid) return e.getKey();
        }
        throw new IllegalArgumentException("Bid is not registered with this BidManager");
    }

    /** All committed bids in placement order. */
    public List<Bid> getAllBids() { return new ArrayList<>(bidsByPlayer.values()); }

    public boolean hasBid(PlayerId playerId) { return bidsByPlayer.containsKey(playerId); }

    public boolean isBiddingComplete() { return bidsByPlayer.size() == players.size(); }

    /** Strict hierarchical legality check, mirroring the rules previously in BidState. */
    public boolean isLegalBid(BidType chosenBidType) {
        if (chosenBidType == null)
            throw new IllegalArgumentException("BidType cannot be null when checking legality.");

        switch (chosenBidType) {
            case PASS -> { return true; }
            case ACCEPTANCE -> {
                if (highestBid == null || highestBid.getType() != BidType.PROPOSAL) return false;
            }
            case SOLO_PROPOSAL -> { if (!isBiddingComplete()) return false; }
            case TROEL, TROELA -> { return false; } // forced only, never chosen
            default -> {}
        }

        if (highestBid == null) return true;

        int cmp = chosenBidType.compareTo(highestBid.getType());
        if (cmp < 0) return false;
        if (chosenBidType.getCategory() != BidCategory.MISERIE) return cmp != 0;
        return true;
    }

    // ---------------------------------------------------------------------
    // Partner / participant resolution
    // ---------------------------------------------------------------------

    public Optional<PlayerId> findProposer() { return findBidderByType(BidType.PROPOSAL); }
    public Optional<PlayerId> findAcceptor() { return findBidderByType(BidType.ACCEPTANCE); }

    private Optional<PlayerId> findBidderByType(BidType type) {
        return bidsByPlayer.entrySet().stream()
                .filter(e -> e.getValue().getType() == type)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /** All players who placed a bid of exactly the given Miserie type. */
    public List<PlayerId> findMiserieParticipants(BidType miserieType) {
        if (miserieType == null || miserieType.getCategory() != BidCategory.MISERIE)
            throw new IllegalArgumentException("Expected a MISERIE bid type, got " + miserieType);
        return bidsByPlayer.entrySet().stream()
                .filter(e -> e.getValue().getType() == miserieType)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Locates the forced partner for a TROEL/TROELA bidder.
     * - TROEL: partner is the holder of the missing 4th Ace (suit comes from the bid).
     * - TROELA: partner is the player with the highest Heart, excluding the bidder.
     */
    public PlayerId findTroelPartner(PlayerId bidder, BidType troelType, Suit missingAceSuit) {
        Objects.requireNonNull(bidder, "bidder");
        if (troelType == null || troelType.getCategory() != BidCategory.TROEL)
            throw new IllegalArgumentException("Expected TROEL/TROELA, got " + troelType);

        if (troelType == BidType.TROEL) {
            Card missingAce = new Card(missingAceSuit, Rank.ACE);
            return players.stream()
                    .filter(p -> p.hasCard(missingAce))
                    .map(Player::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Troel partner not found. Corrupted deck!"));
        }

        // TROELA
        return players.stream()
                .filter(p -> !p.getId().equals(bidder))
                .max(Comparator.comparing(
                        p -> getHighestRankOfSuit(Suit.HEARTS, p.getHand()),
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Player::getId)
                .orElseThrow(() -> new IllegalStateException("Troela partner not found. Corrupted deck!"));
    }

    // ---------------------------------------------------------------------
    // Forced-bid detection (was BidState.applyForcedBids)
    // ---------------------------------------------------------------------

    /** Returns TROELA if the player has 4 Aces, TROEL if 3, otherwise empty. */
    public Optional<BidType> detectForcedBid(Player player) {
        long aces = player.getHand().stream().filter(c -> c.rank() == Rank.ACE).count();
        if (aces == 4) return Optional.of(BidType.TROELA);
        if (aces == 3) return Optional.of(BidType.TROEL);
        return Optional.empty();
    }

    /** The suit of the single Ace this player is missing (only meaningful when they hold 3 Aces). */
    public Suit findMissingAceSuit(Player player) {
        List<Suit> heldAceSuits = player.getHand().stream()
                .filter(c -> c.rank() == Rank.ACE)
                .map(Card::suit)
                .toList();
        for (Suit s : Suit.values()) {
            if (!heldAceSuits.contains(s)) return s;
        }
        throw new IllegalStateException("Player holds all 4 Aces; no missing Ace suit.");
    }

    // ---------------------------------------------------------------------
    // The thing Round actually asks for at the end of bidding
    // ---------------------------------------------------------------------

    /**
     * Resolves the full attacking team for the current highest bid.
     * Returns a list of size {@code highestBid.teamSize()} (1 or 2) of PlayerIds.
     * For MISERIE returns every player who joined the same Miserie contract.
     */
    public List<PlayerId> resolveBiddingTeam() {
        if (highestBid == null || highestBidder == null)
            throw new IllegalStateException("No highest bid to resolve a team for.");

        BidType type = highestBid.getType();
        return switch (type.getCategory()) {
            case PASS -> List.of(highestBidder);
            case PROPOSAL, ACCEPTANCE -> {
                PlayerId proposer = findProposer().orElseThrow(
                        () -> new IllegalStateException("Acceptance without a proposer"));
                PlayerId acceptor = findAcceptor().orElseThrow(
                        () -> new IllegalStateException("Proposal without an acceptor"));
                yield List.of(proposer, acceptor);
            }
            case TROEL -> {
                Suit missing = (highestBid instanceof TroelBid tb) ? tb.trumpSuit() : null;
                PlayerId partner = findTroelPartner(highestBidder, type, missing);
                yield List.of(highestBidder, partner);
            }
            case MISERIE -> findMiserieParticipants(type);
            case SOLO, ABONDANCE -> List.of(highestBidder);
        };
    }
}