package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidManager;
import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.commands.GameCommand;
import base.domain.commands.GameCommand.*;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.results.BidResults.*;
import base.domain.results.GameResult;
import base.domain.turn.BidTurn;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages the Bidding phase of the Whist game.
 * Acts as the active controller, validating bids, applying forced contracts (Troel/Troela),
 * and broadcasting state changes to Observers. Delegates all bid-history reasoning
 * (highest bid, partner lookup, legality, forced detection) to {@link BidManager}.
 *
 * @author Stan Kestens, Tommy Wu
 * @since 01/03/2026
 */
public class BidState extends State {

    /** The bid manager owned by the current Round. Single source of truth for bid history. */
    private final BidManager bidManager;

    private final Suit dealtTrumpSuit;
    private Suit currentTrumpSuit;
    private BidType pendingBidType;
    private Player currentPlayer;

    public BidState(WhistGame game) {
        super(game);
        if (game.getPlayers() == null || game.getPlayers().size() != 4
                || game.getPlayers().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Cannot start BidState: Game must have exactly 4 valid players.");
        }

        Player dealerPlayer = game.getDealerPlayer();
        if (dealerPlayer == null) {
            throw new IllegalStateException("Cannot start BidState: Dealer player is not set.");
        }

        this.currentPlayer = game.getNextPlayer(dealerPlayer);

        this.dealtTrumpSuit = game.dealCards();
        if (this.dealtTrumpSuit == null) {
            throw new IllegalStateException("Dealing cards did not yield a valid trump suit.");
        }
        this.currentTrumpSuit = dealtTrumpSuit;

        // Round must be initialised AFTER dealing so the BidManager can see hands for forced-bid detection.
        game.initializeNextRound(currentPlayer);
        this.bidManager = game.getCurrentRound().getBidManager();

        getGame().notifyRoundStarted();

        applyForcedBids();

        // If the player who was supposed to go first is now locked in by a forced bid, skip them.
        if (bidManager.hasBid(currentPlayer.getId())) {
            updateCurrentPlayer();
        }
    }

    // =========================================================================
    // Forced bid registration
    // =========================================================================

    private void applyForcedBids() {
        for (Player player : getGame().getPlayers()) {
            BidType forced = bidManager.detectForcedBid(player);
            if (forced == null) continue;

            Suit suit = (forced == BidType.TROEL)
                    ? bidManager.findMissingAceSuit(player)
                    : Suit.HEARTS;
            commitBid(player.getId(), forced, suit);
            break;
        }
    }

    // =========================================================================
    // State Execution & Transitions
    // =========================================================================

    @Override
    public StateStep executeState() {
        return StateStep.stay(buildBidTurnResult());
    }

    @Override
    public StateStep executeState(GameCommand command) {
        if (command == null) throw new IllegalArgumentException("GameCommand cannot be null.");

        GameResult earlyReturn = switch (command) {
            case BidCommand b -> {
                Bid current = bidManager.getHighestBid();
                if (bidManager.isBiddingComplete() && current != null && current.getType() == BidType.PROPOSAL) {
                    yield handleRejectedProposal(b.bid());
                }
                yield handleBidCommand(b.bid(), b.suit());
            }
            case SuitCommand s -> handleSuitCommand(s.suit());
            default -> throw new IllegalStateException("Unexpected command type: " + command);
        };

        if (earlyReturn != null) return toStep(earlyReturn);
        if (bidManager.isBiddingComplete()) return toStep(handleEndOfBidding());
        return StateStep.stay(buildBidTurnResult());
    }

    @Override
    public State nextState() {
        if (!bidManager.isBiddingComplete()) {
            throw new IllegalStateException(
                    "State violation: Cannot transition to next state before bidding is complete.");
        }

        Bid highest = bidManager.getHighestBid();
        if (highest == null || highest.getType() == BidType.PASS) {
            getGame().getCurrentRound().abortWithAllPass(bidManager.getAllBids());
            return new BidState(getGame());
        }
        setRoundReadyForPlayState();
        return new PlayState(getGame());
    }

    // =========================================================================
    // PRIMARY COMMAND HANDLERS
    // =========================================================================

    private GameResult handleBidCommand(BidType chosenBidType, Suit preSuppliedSuit) {
        if (chosenBidType == null)
            throw new IllegalArgumentException("chosenBidType cannot be null.");
        if (bidManager.isBiddingComplete())
            throw new IllegalStateException("State violation: Cannot handle new bid, bidding is already complete.");
        if (pendingBidType != null)
            throw new IllegalStateException("State violation: Cannot process a new bid while waiting for a suit selection.");
        if (!bidManager.isLegalBid(chosenBidType))
            throw new IllegalArgumentException("State violation: Bid " + chosenBidType + " is not legal in the current context.");

        if (chosenBidType.getRequiresSuit()) {
            return processSuitRequirement(chosenBidType, preSuppliedSuit);
        }

        commitBid(currentPlayer.getId(), chosenBidType, null);
        updateCurrentPlayer();
        return null;
    }

    private GameResult handleSuitCommand(Suit suit) {
        if (suit == null) throw new IllegalArgumentException("Suit cannot be null.");
        if (pendingBidType == null)
            throw new IllegalStateException("State violation: Received SuitCommand but no pending bid requires a suit.");

        commitBid(currentPlayer.getId(), pendingBidType, suit);
        updateCurrentPlayer();
        this.pendingBidType = null;
        return null;
    }

    private GameResult handleRejectedProposal(BidType decision) {
        if (decision == null) throw new IllegalArgumentException("Decision cannot be null.");
        if (decision != BidType.PASS && decision != BidType.SOLO_PROPOSAL)
            throw new IllegalArgumentException("Rejected proposal decision must be PASS or SOLO_PROPOSAL.");

        Bid current = bidManager.getHighestBid();
        if (current == null || current.getType() != BidType.PROPOSAL)
            throw new IllegalStateException("State violation: Not in a rejected proposal context.");

        PlayerId proposerId = bidManager.findProposer();
        if (proposerId == null)
            throw new IllegalStateException("Critical error: PROPOSAL bid not found in memory.");

        // Switch the active player back to the proposer and let them choose their fallback bid.
        this.currentPlayer = getGame().getPlayerById(proposerId);
        bidManager.invalidateProposal();
        commitBid(proposerId, decision, null);

        return new BiddingCompleted();
    }

    private GameResult handleEndOfBidding() {
        Bid highest = bidManager.getHighestBid();
        if (highest != null && highest.getType() == BidType.PROPOSAL) {
            PlayerId proposerId = bidManager.findProposer();
            if (proposerId == null)
                throw new IllegalStateException("Critical error: Proposal bid missing at end of bidding.");
            return new ProposalRejected(getGame().getPlayerById(proposerId).getName());
        }
        return new BiddingCompleted();
    }

    private void setRoundReadyForPlayState() {
        Bid winningBid = bidManager.getHighestBid();
        if (winningBid == null)
            throw new IllegalStateException("Cannot prepare play state: No winning bid was determined.");
        if (winningBid.getType() == BidType.PASS)
            throw new IllegalStateException("Cannot prepare play state: winning bid is PASS.");
        if (winningBid.getType() == BidType.PROPOSAL)
            throw new IllegalStateException("Cannot prepare play state: highest bid is an unresolved rejected proposal.");

        Player firstPlayer = determineFirstPlayerToLead(winningBid);
        getGame().getCurrentRound().startPlayPhase(
                bidManager.getAllBids(), winningBid, this.currentTrumpSuit, firstPlayer);

        List<PlayerId> biddingTeam = bidManager.resolveBiddingTeam();
        getGame().notifyBiddingFinalized(winningBid.getType(), biddingTeam);
    }

    // =========================================================================
    // SECONDARY MUTATORS
    // =========================================================================

    /**
     * Single side-effect entry point for placing a bid: registers it in the manager,
     * broadcasts the BidTurn, and refreshes the current trump suit if the highest bid changed.
     */
    private void commitBid(PlayerId playerId, BidType bidType, Suit trumpSuit) {
        Bid previousHighest = bidManager.getHighestBid();
        Bid placed = bidManager.placeBid(playerId, bidType, trumpSuit);

        getGame().notifyBidPlaced(new BidTurn(playerId, bidType));

        Bid newHighest = bidManager.getHighestBid();
        if (newHighest != null && newHighest != previousHighest) {
            this.currentTrumpSuit = placed.determineTrump(dealtTrumpSuit);
            getGame().notifyTrumpDetermined(currentTrumpSuit);
        }
    }

    private void updateCurrentPlayer() {
        if (bidManager.isBiddingComplete()) return;
        do {
            this.currentPlayer = getGame().getNextPlayer(this.currentPlayer);
        } while (!bidManager.isBiddingComplete() && bidManager.hasBid(this.currentPlayer.getId()));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private GameResult processSuitRequirement(BidType chosenBidType, Suit preSuppliedSuit) {
        if (chosenBidType == null)
            throw new IllegalArgumentException("chosenBidType cannot be null when processing suit requirement.");
        if (preSuppliedSuit != null) {
            commitBid(currentPlayer.getId(), chosenBidType, preSuppliedSuit);
            updateCurrentPlayer();
            return null;
        }
        if (pendingBidType != null)
            throw new IllegalStateException("State violation: pendingBidType is already set.");
        this.pendingBidType = chosenBidType;
        return new SuitSelectionRequired(currentPlayer.getName(), chosenBidType, Suit.values());
    }

    /**
     * Decides who leads the first trick of the play phase.
     * - Solo / Abondance: the bidder leads.
     * - Troel / Troela: the forced partner leads.
     * - Otherwise: left of the dealer.
     */
    private Player determineFirstPlayerToLead(Bid winningBid) {
        if (winningBid == null) throw new IllegalArgumentException("Winning bid cannot be null.");

        WhistGame game = this.getGame();
        BidCategory category = winningBid.getType().getCategory();

        PlayerId bidder = bidManager.getHighestBidder();
        if (bidder == null)
            throw new IllegalStateException("Highest bidder unknown when resolving first leader.");

        if (category == BidCategory.ABONDANCE || category == BidCategory.SOLO) {
            return game.getPlayerById(bidder);
        }

        if (category == BidCategory.TROEL) {
            for (PlayerId id : bidManager.resolveBiddingTeam()) {
                if (!id.equals(bidder)) return game.getPlayerById(id);
            }
            throw new IllegalStateException("No partner found in the Troel team!");
        }

        return game.getNextPlayer(game.getDealerPlayer());
    }

    private BidTurnResult buildBidTurnResult() {
        Bid current = bidManager.getHighestBid();

        String highestBidderName = null;
        if (current != null && current.getType() != BidType.PASS) {
            PlayerId highestBidderId = bidManager.getHighestBidder();
            if (highestBidderId != null) {
                highestBidderName = getGame().getPlayerById(highestBidderId).getName();
            }
        }

        return new BidTurnResult(
                currentPlayer.getName(),
                currentTrumpSuit,
                current,
                highestBidderName,
                getLegalBids(),
                currentPlayer.getHand(),
                currentPlayer
        );
    }

    private StateStep toStep(GameResult result) {
        return switch (result) {
            case BiddingCompleted ignored -> StateStep.transition(result);
            default -> StateStep.stay(result);
        };
    }

    private List<BidType> getLegalBids() {
        List<BidType> legalBids = new ArrayList<>();
        for (BidType bidType : BidType.values()) {
            if (bidManager.isLegalBid(bidType)) legalBids.add(bidType);
        }
        return legalBids;
    }
}