package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.bid.PassBid;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.events.*;
import base.domain.player.Player;
import base.domain.events.bidevents.*;
import java.util.ArrayList;
import java.util.List;
/**
 * Manages the Bidding phase of the Whist game.
 *
 * @author Stan Kestens, Tommy Wu
 * @since 01/03/2026
 */
public class BidState extends State {
    private final List<Bid> bids;
    private BidType currentHighestBidType;
    private Player currentPlayer;
    private Suit trumpSuit;
    private BidType pendingBidType;

    /**
     * Initializes a new bidding round.
     * 
     * @param game The main game instance.
     */
    public BidState(WhistGame game) {
        super(game);
        this.bids = new ArrayList<>();
        this.currentHighestBidType = null;
        Player dealerPlayer = game.getDealerPlayer();

        int dealerIdx = game.getPlayers().indexOf(dealerPlayer);
        this.currentPlayer = game.getPlayers().get((dealerIdx + 1) % game.getPlayers().size());

        this.trumpSuit = game.dealCards();
        game.initializeNextRound(currentPlayer);
        applyForcedBids();

        // If the starting player got forced into Troel, skip them immediately!
        if (bids.stream().anyMatch(bid -> bid.getPlayer().equals(currentPlayer))) {
            updateCurrentPlayer();
        }
    }

    /**
     * Scans all players for 3 or 4 Aces. If found, automatically registers the
     * forced Troel/Troela bid for that player before normal bidding begins.
     */
    private void applyForcedBids() {
        for (Player player : getGame().getPlayers()) {

            long aceCount = player.getHand().stream()
                    .filter(card -> card.rank() == Rank.ACE)
                    .count();

            if (aceCount == 3) {
                Bid forcedBid = BidType.TROEL.instantiate(player, null);
                commitBid(forcedBid);
                break;
            }
            else if (aceCount == 4) {
                Bid forcedBid = BidType.TROELA.instantiate(player, null);
                commitBid(forcedBid);
                break;
            }
        }
    }

    /**
     * Processes bidding actions. Handles suit selection, rejected proposals,
     * and automates bot passing.
     * 
     * @param action The user's input (NumberAction for bid/suit choice).
     * @return GameEvent
     */
    @Override
    public GameEvent<?> executeState(GameAction action) {
        if (action instanceof NumberAction(int choice)) {
            // Context A: Resolving a proposal that no one accepted
            if (isBiddingComplete() && currentHighestBidType == BidType.PROPOSAL) {
                return handleRejectedProposal(choice);
            }

            // Context B/C handled by pending bid state
            GameEvent<?> followUpOrError = switch (this.pendingBidType) {
                case null -> handleBidInput(choice);
                default -> handleSuitInput(choice);
            };
            if (followUpOrError != null)
                return followUpOrError;
        }

        // Fast-forward BOT turns
        while (!currentPlayer.getRequiresConfirmation() && !isBiddingComplete()) {
            commitBid(new PassBid(currentPlayer));
            updateCurrentPlayer();
        }

        if (isBiddingComplete())
            return handleEndOfBidding();

        return new BidTurnEvent(currentPlayer.getName(), trumpSuit, currentHighestBidType,
                BidType.values(), currentPlayer.getHand());
    }

    /**
     * Determines the next state. If everyone passed, reshuffles for a new BidState.
     * Otherwise, prepares the round for PlayState.
     */
    @Override
    public State nextState() {
        if (currentHighestBidType == BidType.PASS) {
            getGame().getCurrentRound().abortWithAllPass(bids);
            return new BidState(getGame());
        }
        setRoundReadyForPlayState();
        return new PlayState(getGame());
    }

    /**
     * Validates and commits a bid selection.
     * 
     * @return GameEvent
     */
    private GameEvent<?> handleBidInput(int choice) {
        BidType[] allBids = BidType.values();
        if (choice < 1 || choice > allBids.length)
            return new ErrorEvent(1, allBids.length);

        BidType chosenBidType = allBids[choice - 1];
        if (!isLegalBidType(chosenBidType))
            return new ErrorEvent(1, allBids.length);

        if (chosenBidType.getRequiresSuit()) {
            this.pendingBidType = chosenBidType;
            return new SuitPromptEvent(currentPlayer.getName(), chosenBidType, Suit.values());
        }

        commitBid(chosenBidType.instantiate(currentPlayer, null));
        updateCurrentPlayer();
        return null;
    }

    /**
     * Validates and commits the suit for a pending bid.
     * 
     * @return GameEvent
     */
    private GameEvent<?> handleSuitInput(int choice) {
        Suit[] allSuits = Suit.values();
        if (choice < 1 || choice > allSuits.length)
            return new ErrorEvent(1, allSuits.length);

        commitBid(pendingBidType.instantiate(currentPlayer, allSuits[choice - 1]));
        updateCurrentPlayer();
        this.pendingBidType = null;
        return null;
    }

    /**
     * Processes the choice of a proposer when their proposal is rejected.
     * 
     * @return GameEvent
     */
    private GameEvent<?> handleRejectedProposal(int choice) {
        BidType decision = (choice == 1) ? BidType.PASS : (choice == 2) ? BidType.SOLO_PROPOSAL : null;
        if (decision == null)
            return new ErrorEvent(1, 2);

        replaceProposalBid(decision);
        this.currentHighestBidType = decision;
        return new BiddingCompleteEvent();
    }

    /**
     * Handles the ending of the bidding.
     * 
     * @return GameEvent
     */
    private GameEvent<?> handleEndOfBidding() {
        if (currentHighestBidType == BidType.PROPOSAL) {
            return new RejectedProposalEvent(findBid(BidType.PROPOSAL).getPlayer().getName());
        }
        return new BiddingCompleteEvent();
    }

    /**
     * Updates state variables and advances the turn.
     */
    private void commitBid(Bid finalizedBid) {
        this.bids.add(finalizedBid);
        if (currentHighestBidType == null || finalizedBid.getType().compareTo(currentHighestBidType) > 0) {
            currentHighestBidType = finalizedBid.getType();
        }
        trumpSuit = finalizedBid.determineTrump(trumpSuit);
    }

    /**
     * Advances the turn to the next player. Skips players who already have a forced bid.
     */
    private void updateCurrentPlayer() {
        List<Player> players = getGame().getPlayers();

        do {
            this.currentPlayer = players.get((players.indexOf(currentPlayer) + 1) % players.size());
            // Keep skipping IF the bidding isn't done AND the player we landed on already has a bid!
        } while (!isBiddingComplete() && bids.stream().anyMatch(bid -> bid.getPlayer().equals(currentPlayer)));
    }

    /**
     * Updating the proposal bid
     * 
     * @param chosenBidType is the bid that was chosen
     */
    private void replaceProposalBid(BidType chosenBidType) {
        Bid proposalBid = findBid(BidType.PROPOSAL);
        int index = bids.indexOf(proposalBid);
        bids.set(index, chosenBidType.instantiate(proposalBid.getPlayer(), null));
    }

    /**
     * Enforces the hierarchical rules of Whist bidding.
     * 
     * @return boolean if the new bid is legal
     */
    private boolean isLegalBidType(BidType chosenBidType) {
        if (chosenBidType == BidType.PASS)
            return true;
        if (chosenBidType == BidType.ACCEPTANCE && currentHighestBidType != BidType.PROPOSAL)
            return false;
        if (chosenBidType == BidType.SOLO_PROPOSAL && !isBiddingComplete())
            return false;
        if (currentHighestBidType == null)
            return true;

        int comparison = chosenBidType.compareTo(currentHighestBidType);
        if (comparison < 0)
            return false;
        // Miserie can be bid over another miserie even if ranks are equal
        if (chosenBidType.getCategory() != BidCategory.MISERIE) {
            return comparison != 0;
        }
        return true;
    }

    /**
     * checks if bidding is complete
     * 
     * @return boolean true if trick is complete
     */
    private boolean isBiddingComplete() {
        return this.bids.size() == getGame().getPlayers().size();
    }

    /**
     *
     * @param bidType what bid we want to find
     * @return Bid that is linked to the bidType
     */
    private Bid findBid(BidType bidType) {
        return bids.stream().filter(b -> b.getType() == bidType).findFirst().orElse(null);
    }

    /** Prepares the Round object with the winning bidder and trump suit. */
    private void setRoundReadyForPlayState() {
        WhistGame game = this.getGame();
        List<Player> players = game.getPlayers();
        Player firstPlayer = players.get((players.indexOf(game.getDealerPlayer()) + 1) % 4);

        if (this.currentHighestBidType.getCategory() == BidCategory.ABONDANCE ||
                this.currentHighestBidType.getCategory() == BidCategory.SOLO) {
            firstPlayer = findBid(currentHighestBidType).getPlayer();
        }
        else if (this.currentHighestBidType.getCategory() == BidCategory.TROEL) {
            Bid troelBid = findBid(currentHighestBidType);
            // Find the partner by filtering out the original bidder from the team list
            firstPlayer = troelBid.getTeam(this.bids, players).stream()
                    .filter(p -> !p.equals(troelBid.getPlayer()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No partner found in the Troel team!"));
        }

        Bid winningBid = findBid(currentHighestBidType);
        game.getCurrentRound().startPlayPhase(this.bids, winningBid, this.trumpSuit, firstPlayer);
    }

}