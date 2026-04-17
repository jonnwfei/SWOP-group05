package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Card;
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

import java.util.*;

/**
 * Manages the Bidding phase of the Whist game.
 * Acts as the active controller, validating bids, applying forced contracts (Troel/Troela),
 * and broadcasting state changes to Observers.
 *
 * @author Stan Kestens, Tommy Wu
 * @since 01/03/2026
 */
public class BidState extends State {
    private final List<Bid> bids;
    private final Set<PlayerId> playersWhoTookTurn;
    private final Suit dealtTrumpSuit;

    private BidType currentHighestBidType;
    private Suit currentTrumpSuit;
    private BidType pendingBidType;
    private Player currentPlayer;

    /**
     * Initializes a new bidding round, deals cards, determines the initial trump
     * suit, and processes any immediate forced bids (Troel/Troela).
     * @param game The main game instance.
     * @throws IllegalArgumentException if the game is null, or if the player list is invalid.
     * @throws IllegalStateException    if the dealer is missing or dealing fails.
     */
    public BidState(WhistGame game) {
        super(game);
        if (game.getPlayers() == null || game.getPlayers().size() != 4 || game.getPlayers().contains(null)) {
            throw new IllegalArgumentException("Cannot start BidState: Game must have exactly 4 valid players.");
        }

        this.bids = new ArrayList<>();
        this.playersWhoTookTurn = new HashSet<>();
        this.currentHighestBidType = null;

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

        game.initializeNextRound(currentPlayer);

        // Broadcast that the round has officially started
        getGame().notifyRoundStarted();

        // Register forced bids. The forced player will be locked out of manual bidding.
        applyForcedBids();

        // If the player who was supposed to go first was the forced bidder, skip them
        if (playersWhoTookTurn.contains(currentPlayer.getId())) {
            updateCurrentPlayer();
        }
    }

    /**
     * Scans all players' hands for 3 or 4 Aces. If found, automatically registers
     * the forced Troel or Troela bid for that player.
     * The forced bidder is immediately marked as having taken their turn.
     */
    private void applyForcedBids() {
        for (Player player : getGame().getPlayers()) {
            long aceCount = countAces(player);

            if (aceCount == 3) {
                Suit missingSuit = findMissingAceSuit(player);
                Bid forcedBid = BidType.TROEL.instantiate(player.getId(), missingSuit);

                commitBid(forcedBid);
                playersWhoTookTurn.add(player.getId());
                break;
            } else if (aceCount == 4) {
                Bid forcedBid = BidType.TROELA.instantiate(player.getId(), Suit.HEARTS);

                commitBid(forcedBid);
                playersWhoTookTurn.add(player.getId());
                break;
            }
        }
    }

    // =========================================================================
    // State Execution & Transitions
    // =========================================================================

    /**
     * Entry point into the bidding phase. Emits the initial Turn Result without
     * processing any commands.
     *
     * @return A StateStep instructing the engine to stay in this state.
     */
    @Override
    public StateStep executeState() {
        return StateStep.stay(buildBidTurnResult());
    }

    /**
     * Processes incoming bidding commands from the adapter.
     * Handles standard bids, suit selection for bids that require it, rejected
     * proposals, and safely fast-forwards through automated Bot turns.
     *
     * @param command The domain command from the adapter.
     * @return StateStep detailing whether to transition or stay in the current state.
     * @throws IllegalArgumentException if the provided command is null.
     * @throws IllegalStateException if an unexpected command type is provided.
     */
    @Override
    public StateStep executeState(GameCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("GameCommand cannot be null.");
        }

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

        if (earlyReturn != null) return toStep(earlyReturn);
        if (isBiddingComplete()) return toStep(handleEndOfBidding());
        return StateStep.stay(buildBidTurnResult());
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
            throw new IllegalStateException("State violation: Cannot transition to next state before bidding is complete.");
        }

        if (currentHighestBidType == BidType.PASS) {
            getGame().getCurrentRound().abortWithAllPass(bids);
            return new BidState(getGame());
        }
        setRoundReadyForPlayState();
        return new PlayState(getGame());
    }

    // =========================================================================
    // PRIMARY COMMAND HANDLERS & WORKFLOWS
    // =========================================================================

    /**
     * Handles a standard bid placement by a player.
     *
     * @param chosenBidType The bid type selected by the player.
     * @param preSuppliedSuit An optional suit provided by bots/automation.
     * @return null to continue normal flow, or a GameResult if further action is required.
     * @throws IllegalArgumentException if the chosen bid type is null or illegal.
     * @throws IllegalStateException if bidding is completed or waiting on a suit.
     */
    private GameResult handleBidCommand(BidType chosenBidType, Suit preSuppliedSuit) {
        if (chosenBidType == null) {
            throw new IllegalArgumentException("chosenBidType cannot be null.");
        }
        if (isBiddingComplete()) {
            throw new IllegalStateException("State violation: Cannot handle new bid, bidding is already complete.");
        }
        if (pendingBidType != null) {
            throw new IllegalStateException("State violation: Cannot process a new bid while waiting for a suit selection.");
        }

        if (!isLegalBidType(chosenBidType)) {
            throw new IllegalArgumentException("State violation: Bid " + chosenBidType + " is not legal in the current context.");
        }

        if (chosenBidType.getRequiresSuit()) {
            return processSuitRequirement(chosenBidType, preSuppliedSuit);
        }

        commitBid(chosenBidType.instantiate(currentPlayer.getId(), null));
        playersWhoTookTurn.add(currentPlayer.getId());
        updateCurrentPlayer();
        return null;
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
        playersWhoTookTurn.add(currentPlayer.getId());
        updateCurrentPlayer();
        this.pendingBidType = null;
        return null;
    }

    /**
     * Processes the choice of a proposer when their initial proposal is rejected.
     *
     * @param decision The bid type the proposer chose (must be PASS or SOLO_PROPOSAL).
     * @return GameResult indicating bidding has fully completed.
     * @throws IllegalArgumentException if the decision is null or invalid.
     * @throws IllegalStateException if the context does not involve a rejected proposal.
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

        Bid proposalBid = findBid(BidType.PROPOSAL);
        if (proposalBid == null) {
            throw new IllegalStateException("Critical error: PROPOSAL bid not found in memory.");
        }

        Player originalBidder = getGame().getPlayerById(proposalBid.getPlayerId());
        this.currentPlayer = originalBidder;

        removeProposalBid();
        Bid chosenBid = decision.instantiate(originalBidder.getId(), null);
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
            if (proposalBid == null) {
                throw new IllegalStateException("Critical error: Proposal bid missing at end of bidding.");
            }
            Player proposer = getGame().getPlayerById(proposalBid.getPlayerId());
            return new ProposalRejected(proposer.getName());
        }
        return new BiddingCompleted();
    }

    /**
     * Injects the finalized bidding context into the Round object to prepare it for the PlayState.
     * Computes the primary player, the final trump suit, and the winning bid instance.
     *
     * @throws IllegalStateException if a winning bid cannot be resolved or is an unresolved proposal.
     */
    private void setRoundReadyForPlayState() {
        if (this.currentHighestBidType == null || this.currentHighestBidType == BidType.PASS) {
            throw new IllegalStateException("Cannot prepare play state: No winning bid was determined, or it was PASS.");
        }
        if (this.currentHighestBidType == BidType.PROPOSAL) {
            throw new IllegalStateException("Cannot prepare play state: highest bid is an unresolved rejected proposal.");
        }

        Bid winningBid = findBid(currentHighestBidType);
        if (winningBid == null) {
            throw new IllegalStateException("Critical error: The declared winning bid (" + currentHighestBidType + ") is not present in the bids list.");
        }

        Player firstPlayer = determineFirstPlayerToLead(winningBid);

        getGame().getCurrentRound().startPlayPhase(this.bids, winningBid, this.currentTrumpSuit, firstPlayer);
    }

    // =========================================================================
    // SECONDARY STATE MUTATORS
    // =========================================================================

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
            throw new IllegalStateException("State violation: Cannot commit bid. The maximum number of bids has already been reached.");
        }

        this.bids.add(finalizedBid);

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
     */
    private void updateCurrentPlayer() {
        if (isBiddingComplete()) return;

        do {
            this.currentPlayer = getGame().getNextPlayer(this.currentPlayer);
        } while (!isBiddingComplete() && playersWhoTookTurn.contains(this.currentPlayer.getId()));
    }

    /**
     * Safely removes the PROPOSAL bid from memory and resets the current highest bid to PASS.
     * Used when a proposal is rejected.
     */
    private void removeProposalBid() {
        Bid proposalBid = findBid(BidType.PROPOSAL);
        if (proposalBid != null) {
            bids.remove(proposalBid);
        }
        currentHighestBidType = BidType.PASS;
    }

    // =========================================================================
    // HELPER FUNCTIONS
    // =========================================================================

    /**
     * Asks the player for the exact count of Aces in their hand without exposing the entire hand.
     * @param player The player to interrogate.
     * @return the number of Aces found.
     * @throws IllegalArgumentException if player is null.
     */
    private long countAces(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null when counting Aces.");
        }
        return Arrays.stream(Suit.values())
                .filter(suit -> player.hasCard(new Card(suit, Rank.ACE)))
                .count();
    }

    /**
     * Asks the player which Ace suit they are missing (used exclusively for Troel).
     * * @param player The player to interrogate.
     * @return The Suit of the missing Ace.
     * @throws IllegalArgumentException if player is null.
     * @throws IllegalStateException if the missing suit cannot be logically resolved.
     */
    private Suit findMissingAceSuit(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null when finding missing Ace suit.");
        }
        return Arrays.stream(Suit.values())
                .filter(suit -> !player.hasCard(new Card(suit, Rank.ACE)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Critical Error: Could not find the missing Ace suit for Troel."));
    }

    /**
     * Processes bids that require the user or bot to declare a specific suit.
     * @param chosenBidType The base bid type chosen.
     * @param preSuppliedSuit The suit chosen, if provided in advance.
     * @return A GameResult demanding the user select a suit, or null if successfully processed.
     * @throws IllegalArgumentException if the chosenBidType is null.
     * @throws IllegalStateException if another bid is already pending suit resolution.
     */
    private GameResult processSuitRequirement(BidType chosenBidType, Suit preSuppliedSuit) {
        if (chosenBidType == null) {
            throw new IllegalArgumentException("chosenBidType cannot be null when processing suit requirement.");
        }
        if (preSuppliedSuit != null) {
            commitBid(chosenBidType.instantiate(currentPlayer.getId(), preSuppliedSuit));
            playersWhoTookTurn.add(currentPlayer.getId());
            updateCurrentPlayer();
            return null;
        }
        if (pendingBidType != null) {
            throw new IllegalStateException("State violation: pendingBidType is already set.");
        }
        this.pendingBidType = chosenBidType;
        return new SuitSelectionRequired(currentPlayer.getName(), chosenBidType, Suit.values());
    }

    /**
     * Determines which player leads the first trick, depending on the constraints of the winning contract.
     * @param winningBid The finalized winning bid.
     * @return The Player object representing the trick leader.
     * @throws IllegalArgumentException if the winning bid is null.
     * @throws IllegalStateException if a required partner for a team bid cannot be found.
     */
    private Player determineFirstPlayerToLead(Bid winningBid) {
        if (winningBid == null) {
            throw new IllegalArgumentException("Winning bid cannot be null when determining first player.");
        }

        WhistGame game = this.getGame();
        List<Player> players = game.getPlayers();

        if (winningBid.getType().getCategory() == BidCategory.ABONDANCE ||
                winningBid.getType().getCategory() == BidCategory.SOLO) {
            return game.getPlayerById(winningBid.getPlayerId());
        } else if (winningBid.getType().getCategory() == BidCategory.TROEL) {
            PlayerId partnerId = winningBid.getTeam(this.bids, players).stream()
                    .filter(p -> !p.equals(winningBid.getPlayerId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No partner found in the Troel team!"));
            return game.getPlayerById(partnerId);
        }

        return game.getNextPlayer(game.getDealerPlayer()); // Default behavior
    }

    // =========================================================================
    // UTILITY, VALIDATION & QUERIES
    // =========================================================================

    /**
     * Constructs the standard payload requested by the UI layer to render the user's turn.
     * @return A populated BidTurnResult object.
     */
    private BidTurnResult buildBidTurnResult() {
        return new BidTurnResult(
                currentPlayer.getName(),
                currentTrumpSuit,
                currentHighestBidType,
                getLegalBids(),
                currentPlayer.getHand(),
                currentPlayer
        );
    }

    /**
     * Maps a GameResult into the appropriate StateStep structure for the State Machine wrapper.
     * @param result The outcome of the current command execution.
     * @return A StateStep representing transition instructions.
     */
    private StateStep toStep(GameResult result) {
        return switch (result) {
            case BiddingCompleted ignored -> StateStep.transition(result);
            default -> StateStep.stay(result);
        };
    }

    /**
     * Enforces the strict hierarchical rules of Whist bidding.
     *
     * @param chosenBidType The type of bid the player wants to place.
     * @return true if the bid type is legally allowed given the current highest bid.
     * @throws IllegalArgumentException if the chosen bid type is null.
     */
    private boolean isLegalBidType(BidType chosenBidType) {
        if (chosenBidType == null) {
            throw new IllegalArgumentException("BidType cannot be null when checking legality.");
        }

        switch (chosenBidType) {
            case PASS -> { return true; }
            case ACCEPTANCE -> { if (currentHighestBidType != BidType.PROPOSAL) return false; }
            case SOLO_PROPOSAL -> { if (!isBiddingComplete()) return false; }
            case TROEL, TROELA -> { return false; }
            default -> {}
        }

        if (currentHighestBidType == null) return true;

        int comparison = chosenBidType.compareTo(currentHighestBidType);
        if (comparison < 0) return false;
        if (chosenBidType.getCategory() != BidCategory.MISERIE)
            return comparison != 0;

        return true;
    }

    /**
     * Checks if the bidding cycle has concluded based on turn tracking.
     *
     * @return true if all players have submitted a valid turn/bid.
     */
    private boolean isBiddingComplete() {
        return this.playersWhoTookTurn.size() == getGame().getPlayers().size();
    }

    /**
     * Utility method to find a specific bid type within the current bids memory.
     *
     * @param bidType the bid type to search for.
     * @return The Bid object linked to the bidType, or null if not found.
     * @throws IllegalArgumentException if the requested bidType is null.
     */
    private Bid findBid(BidType bidType) {
        if (bidType == null) {
            throw new IllegalArgumentException("BidType cannot be null when finding a bid.");
        }
        return bids.stream().filter(b -> b.getType() == bidType).findFirst().orElse(null);
    }

    /**
     * Returns all legal bid types for the current player in the current bidding context.
     * * @return A list of legally allowed BidTypes.
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