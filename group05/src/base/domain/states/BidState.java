package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.commands.BidCommand;
import base.domain.commands.GameCommand;
import base.domain.commands.SuitCommand;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.results.BidTurnResult;
import base.domain.results.BiddingCompleted;
import base.domain.results.GameResult;
import base.domain.results.ProposalRejected;
import base.domain.results.SuitSelectionRequired;
import base.domain.turn.BidTurn;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Bidding phase of the Whist game.
 * Acts as the active controller, validating bids and broadcasting state changes to Observers.
 *
 * @author Stan Kestens, Tommy Wu
 * @since 01/03/2026
 */
public class BidState extends State {
    private final List<Bid> bids;
    private BidType currentHighestBidType;
    private Player currentPlayer;
    private final Suit dealtTrumpSuit;
    private Suit currentTrumpSuit;
    private BidType pendingBidType;

    /**
     * Initializes a new bidding round, deals cards, determines the initial trump
     * suit,
     * and processes any immediate forced bids (Troel/Troela).
     * 
     * @param game The main game instance.
     * @throws IllegalArgumentException if the game is null, or if the player list
     *                                  is null, empty, or not exactly 4 players.
     * @throws IllegalStateException    if the dealer is missing/invalid, or if
     *                                  dealing cards fails to yield a trump suit.
     */
    public BidState(WhistGame game) {
        super(game);
        if (game.getPlayers() == null || game.getPlayers().isEmpty() || game.getPlayers().size() != 4) {
            throw new IllegalArgumentException("Cannot start BidState: Game has no players or no 4 players.");
        }

        this.bids = new ArrayList<>();
        this.currentHighestBidType = null;

        Player dealerPlayer = game.getDealerPlayer();
        if (dealerPlayer == null) {
            throw new IllegalStateException("Cannot start BidState: Dealer player is not set.");
        }

        this.currentPlayer = game.getNextPlayer(game.getDealerPlayer());

        this.dealtTrumpSuit = game.dealCards();
        if (this.dealtTrumpSuit == null) {
            throw new IllegalStateException("Dealing cards did not yield a valid trump suit.");
        }
        this.currentTrumpSuit = dealtTrumpSuit;

        game.initializeNextRound(currentPlayer);

        // Broadcast that the round has officially started with these players
        getGame().notifyRoundStarted();

        applyForcedBids();

        // If the starting player got forced into Troel, skip them immediately!
        if (bids.stream().anyMatch(bid -> bid.getPlayerId().equals(currentPlayer.getId()))) {
            updateCurrentPlayer();
        }
    }

    /**
     * Scans all players' hands for 3 or 4 Aces. If found, automatically registers
     * the
     * forced Troel or Troela bid for that player before normal manual bidding
     * begins.
     */
    private void applyForcedBids() {
        for (Player player : getGame().getPlayers()) {

            long aceCount = player.getHand().stream()
                    .filter(card -> card.rank() == Rank.ACE)
                    .count();

            if (aceCount == 3) {
                Bid forcedBid = BidType.TROEL.instantiate(player.getId(), null);
                commitBid(forcedBid);
                break;
            } else if (aceCount == 4) {
                Bid forcedBid = BidType.TROELA.instantiate(player.getId(), null);
                commitBid(forcedBid);
                break;
            }
        }
    }

    /**
     * Processes incoming bidding commands from the adapter.
     * Handles the initial entry into the bidding phase by returning the current
     * turn result,
     * and then delegates to the overloaded executeState(GameCommand) for any actual
     * commands received.
     *
     * @return GameResult
     * @throws IllegalArgumentException if the provided command is null.
     * @throws IllegalStateException    if an unexpected command type is provided.
     */
    @Override
    public StateStep executeState() {
        // Initial entry — just return current turn
        return StateStep.stay(buildBidTurnResult());
    }

    /**
     * Processes incoming bidding commands from the adapter.
     * Handles standard bids, suit selection for bids that require it, rejected
     * proposals,
     * and safely fast-forwards through automated Bot turns.
     *
     * @param command The domain command from the adapter.
     * @return GameResult
     * @throws IllegalArgumentException if the provided command is null.
     * @throws IllegalStateException if an unexpected command type is provided.
     */
    @Override
    public StateStep executeState(GameCommand command) {
        GameResult earlyReturn = switch (command) {
            case BidCommand b -> {
                if (isBiddingComplete() && currentHighestBidType == BidType.PROPOSAL) {
                    yield handleRejectedProposal(b.bid());
                }
                yield handleBidCommand(b.bid(), b.suit());
            }
            case SuitCommand s -> handleSuitCommand(s.suit());
            default -> throw new IllegalStateException("Unexpected command type: " + command);
        };

        if (earlyReturn != null)
            return toStep(earlyReturn);
        if (isBiddingComplete())
            return toStep(handleEndOfBidding());
        return StateStep.stay(buildBidTurnResult());
    }

    private BidTurnResult buildBidTurnResult() {
        return new BidTurnResult(
                currentPlayer.getName(),
                currentTrumpSuit,
                currentHighestBidType,
                getLegalBids(),
                currentPlayer.getHand(),
                currentPlayer                // added
        );
    }

    private GameResult handleBidCommand(BidType chosenBidType, Suit preSuppliedSuit) {
        if (chosenBidType == null) {
            throw new IllegalArgumentException("chosenBidType cannot be null.");
        }
        if (isBiddingComplete()) {
            throw new IllegalStateException("State violation: Cannot handle new bid, bidding is already complete.");
        }
        if (!isLegalBidType(chosenBidType)) {
            throw new IllegalArgumentException(
                    "State violation: Bid " + chosenBidType + " is not legal in the current context.");
        }

        if (chosenBidType.getRequiresSuit()) {
            if (preSuppliedSuit != null) {
                // Bot pre-supplied the suit — commit immediately, no UI step needed
                commitBid(chosenBidType.instantiate(currentPlayer.getId(), preSuppliedSuit));
                updateCurrentPlayer();
                return null;
            }
            if (pendingBidType != null) {
                throw new IllegalStateException("State violation: pendingBidType is already set.");
            }
            this.pendingBidType = chosenBidType;
            return new SuitSelectionRequired(
                    currentPlayer.getName(),
                    chosenBidType,
                    Suit.values());
        }

        commitBid(chosenBidType.instantiate(currentPlayer.getId(), null));
        updateCurrentPlayer();
        return null;
    }

    /**
     * Determines the next state to transition to after the bidding phase concludes.
     * If everyone passed, the round is aborted and reshuffled for a new BidState.
     * Otherwise, prepares the round and transitions to the PlayState.
     *
     * @return State The next state in the game lifecycle.
     * @throws IllegalStateException if called before all players have successfully bid.
     */
    @Override
    public State nextState() {
        if (!isBiddingComplete()) {
            throw new IllegalStateException(
                    "State violation: Cannot transition to next state before bidding is complete.");
        }

        if (currentHighestBidType == BidType.PASS) {
            getGame().getCurrentRound().abortWithAllPass(bids);
            return new BidState(getGame());
        }
        setRoundReadyForPlayState();
        return new PlayState(getGame());
    }

    private StateStep toStep(GameResult result) {
        return switch (result) {
            case BiddingCompleted ignored -> StateStep.transition(result);
            default -> StateStep.stay(result);
        };
    }

    /**
     * Validates and commits the suit selection for a pending bid (e.g., Abondance, Solo).
     *
     * @param suit The suit chosen by the player.
     * @return null to continue normal flow.
     * @throws IllegalArgumentException if the suit is null.
     * @throws IllegalStateException if no bid is currently pending a suit selection.
     */
    private GameResult handleSuitCommand(Suit suit) {
        if (suit == null) {
            throw new IllegalArgumentException("Suit cannot be null.");
        }
        if (pendingBidType == null) {
            throw new IllegalStateException("State violation: Received SuitCommand but no pending bid requires a suit.");
        }

        commitBid(pendingBidType.instantiate(currentPlayer.getId(), suit));
        this.pendingBidType = null;
        return null;
    }

    /**
     * Processes the choice of a proposer when their initial proposal is rejected.
     *
     * @param decision The bid type the proposer chose (must be PASS or SOLO_PROPOSAL).
     * @return GameResult indicating bidding has fully completed.
     */
    private GameResult handleRejectedProposal(BidType decision) {
        if (decision == null) {
            throw new IllegalArgumentException("Decision cannot be null.");
        }
        if (decision != BidType.PASS && decision != BidType.SOLO_PROPOSAL) {
            throw new IllegalArgumentException("Rejected proposal decision must be PASS or SOLO_PROPOSAL.");
        }
        if (currentHighestBidType != BidType.PROPOSAL) {
            throw new IllegalStateException("State violation: Not in a rejected proposal context.");
        }

        // 1. Find the original proposal to identify the correct bidder
        Bid proposalBid = findBid(BidType.PROPOSAL);
        if (proposalBid == null) {
            throw new IllegalStateException("Critical error: PROPOSAL bid not found.");
        }

        // 2. Capture the original bidder and update the state's pointer
        Player originalBidder = getGame().getPlayerById(proposalBid.getPlayerId());
        this.currentPlayer = originalBidder;

        // 3. Remove the old PROPOSAL and reset the hierarchy
        removeProposalBid();

        // 4. Instantiate the resolution bid using the CORRECT player
        Bid chosenBid = decision.instantiate(originalBidder.getId(), null);

        // 5. Commit the new bid (this updates currentHighestBidType)
        commitBid(chosenBid);

        return new BiddingCompleted();
    }

    /**
     * Handles the logic evaluated immediately after the last player has bid.
     * Resolves pending proposal states or finalizes the bidding phase.
     *
     * @return GameResult prompting for a rejected proposal decision or signifying bidding completion.
     * @throws IllegalStateException if a PROPOSAL was the highest bid but is missing from the records.
     */
    private GameResult handleEndOfBidding() {
        if (currentHighestBidType == BidType.PROPOSAL) {
            Bid proposalBid = findBid(BidType.PROPOSAL);
            if (proposalBid == null)
                throw new IllegalStateException("Critical error: Proposal bid missing at end of bidding.");
            return new ProposalRejected(proposalBid.getPlayer().getName());
            Player proposer = getGame().getPlayerById(proposalBid.getPlayerId());
            return new ProposalRejected(proposer.getName());
        }
        return new BiddingCompleted();
    }

    /**
     * Saves the bid to the state's memory, updating the current highest bid type
     * and the active trump suit if applicable.
     *
     * @param finalizedBid The fully instantiated Bid object to commit.
     * @throws IllegalArgumentException if the finalized bid is null.
     * @throws IllegalStateException if the state attempts to commit more bids than there are players.
     */
    private void commitBid(Bid finalizedBid) {
        if (finalizedBid == null) {
            throw new IllegalArgumentException("Finalized bid cannot be null.");
        }
        if (this.bids.size() >= getGame().getPlayers().size()) {
            throw new IllegalStateException(
                    "State violation: Cannot commit bid. The maximum number of bids has already been reached.");
        }

        this.bids.add(finalizedBid);

        // 1. BROADCAST: Notify all observers that a bid was placed
        BidTurn bidTurn = new BidTurn(finalizedBid.getPlayerId(), finalizedBid.getType());
        getGame().notifyBidPlaced(bidTurn);

        if (currentHighestBidType == null || finalizedBid.getType().compareTo(currentHighestBidType) > 0) {
            currentHighestBidType = finalizedBid.getType();
            currentTrumpSuit = finalizedBid.determineTrump(dealtTrumpSuit);
            getGame().notifyTrumpDetermined(currentTrumpSuit);
        }
    }

    /**
     * Advances the turn to the next player in rotational order.
     * Safely skips players who already possess a forced bid (e.g., Troel).
     *
     * @throws IllegalStateException if an infinite loop is detected (no valid player can be found).
     */
    private void updateCurrentPlayer() {
        if (isBiddingComplete())
            return;

        // Loop until we find a player who hasn't bid yet
        do {
            this.currentPlayer = getGame().getNextPlayer(this.currentPlayer);
        } while (!isBiddingComplete() && hasAlreadyBid(this.currentPlayer));
    }


    private boolean hasAlreadyBid(Player player) {
        return bids.stream().anyMatch(b -> b.getPlayerId().equals(player.getId()));
    }

    /**
     * Safely removes the PROPOSAL bid from memory and resets the current highest bid to PASS.
     * Used when a proposal is rejected.
     */
    private void removeProposalBid() {
        Bid proposalBid = findBid(BidType.PROPOSAL);
        bids.remove(proposalBid);
        currentHighestBidType = BidType.PASS;
    }

    /**
     * Enforces the strict hierarchical rules of Whist bidding.
     *
     * @param chosenBidType The type of bid the player wants to place.
     * @return true if the bid type is legally allowed given the current highest bid.
     */
    private boolean isLegalBidType(BidType chosenBidType) {
        // 1. Check special conditions for specific bid types
        switch (chosenBidType) {
            case PASS -> {
                return true;
            }
            case ACCEPTANCE -> {
                if (currentHighestBidType != BidType.PROPOSAL) return false;
            }
            case SOLO_PROPOSAL -> {
                if (!isBiddingComplete()) return false;
            }
            default -> {// nothing
            }
        }

        if (currentHighestBidType == null) return true;

        int comparison = chosenBidType.compareTo(currentHighestBidType);
        if (comparison < 0) return false;
        if (chosenBidType.getCategory() != BidCategory.MISERIE)
            return comparison != 0;

        return true;
    }

    /**
     * Checks if the bidding cycle has concluded.
     *
     * @return true if all players have submitted exactly one bid.
     */
    private boolean isBiddingComplete() {
        return this.bids.size() == getGame().getPlayers().size();
    }

    /**
     * Utility method to find a specific bid type within the current bids.
     *
     * @param bidType the bid type to search for.
     * @return The Bid object linked to the bidType, or null if not found.
     */
    private Bid findBid(BidType bidType) {
        return bids.stream().filter(b -> b.getType() == bidType).findFirst().orElse(null);
    }

    /**
     * Injects the finalized bidding context into the Round object to prepare it for the PlayState.
     * Computes the primary player, the final trump suit, and the winning bid instance.
     *
     * @throws IllegalStateException if a winning bid cannot be resolved, is an unresolved proposal,
     * or if a required partner for a team bid is missing.
     */
    private void setRoundReadyForPlayState() {
        if (this.currentHighestBidType == null) {
            throw new IllegalStateException("Cannot prepare play state: No winning bid was determined.");
        }
        if (this.currentHighestBidType == BidType.PASS) {
            throw new IllegalStateException("Cannot prepare play state: highest bid is a PASS");
        }
        if (this.currentHighestBidType == BidType.PROPOSAL) {
            throw new IllegalStateException("Cannot prepare play state: highest bid is an unresolved rejected proposal");
        }

        WhistGame game = this.getGame();
        List<Player> players = game.getPlayers();
        Player firstPlayer = game.getNextPlayer(game.getDealerPlayer());

        if (this.currentHighestBidType.getCategory() == BidCategory.ABONDANCE ||
                this.currentHighestBidType.getCategory() == BidCategory.SOLO) {
            firstPlayer = game.getPlayerById(findBid(currentHighestBidType).getPlayerId());
        } else if (this.currentHighestBidType.getCategory() == BidCategory.TROEL) {
            Bid troelBid = findBid(currentHighestBidType);
            // Find the partner by filtering out the original bidder from the team list
            PlayerId playerId = troelBid.getTeam(this.bids, players).stream()
                    .filter(p -> !p.equals(troelBid.getPlayerId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No partner found in the Troel team!"));

            firstPlayer = game.getPlayerById(playerId);
        }

        Bid winningBid = findBid(currentHighestBidType);
        if (winningBid == null) {
            throw new IllegalStateException("Critical error: The declared winning bid (" + currentHighestBidType + ") is not present in the bids list.");
        }
        game.getCurrentRound().startPlayPhase(this.bids, winningBid, this.currentTrumpSuit, firstPlayer);
    }

    /**
     * Returns all legal bid types for a given player in the current bidding context.
     */
    private List<BidType> getLegalBids() {
        List<BidType> legalBids = new ArrayList<>();

        for (BidType bidType : BidType.values()) {
            if (isLegalBidType(bidType)) {
                legalBids.add(bidType);
            }
        }

        return legalBids;
    }
}