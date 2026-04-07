package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.bid.PassBid;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.commands.BidCommand;
import base.domain.commands.ContinueCommand;
import base.domain.commands.GameCommand;
import base.domain.commands.SuitCommand;
import base.domain.player.Player;
import base.domain.results.BidTurnResult;
import base.domain.results.BiddingCompleted;
import base.domain.results.GameResult;
import base.domain.results.ProposalRejected;
import base.domain.results.SuitSelectionRequired;

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
    private final Suit dealtTrumpSuit;
    private Suit currentTrumpSuit;
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

        this.dealtTrumpSuit = game.dealCards();
        this.currentTrumpSuit = dealtTrumpSuit;
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
     * Processes bidding commands. Handles suit selection, rejected proposals,
     * and automates bot passing.
     *
     * @param command The domain command from the adapter.
     * @return GameResult
     */
    @Override
    public GameResult executeState(GameCommand command) {
        GameResult earlyReturn = switch (command) {
            case BidCommand b -> {
                if (isBiddingComplete() && currentHighestBidType == BidType.PROPOSAL) {
                    yield handleRejectedProposal(b.bid());
                }
                yield handleBidInput(b.bid());
            }
            case SuitCommand s -> handleSuitInput(s.suit());
            case ContinueCommand ignored -> null;
        };

        if (earlyReturn != null) return earlyReturn;

        // Fast-forward BOT turns
        while (!currentPlayer.getRequiresConfirmation() && !isBiddingComplete()) {
            commitBid(new PassBid(currentPlayer));
            updateCurrentPlayer();
        }

        if (isBiddingComplete())
            return handleEndOfBidding();

        return new BidTurnResult(
                currentPlayer.getName(),
                currentTrumpSuit,
                currentHighestBidType,
                BidType.values(),
                currentPlayer.getHand()
        );
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
     * @param chosenBidType The bid type chosen by the player.
     * @return GameResult or null to continue normal flow.
     */
    private GameResult handleBidInput(BidType chosenBidType) {
        if (chosenBidType.getRequiresSuit()) {
            this.pendingBidType = chosenBidType;
            return new SuitSelectionRequired(
                    currentPlayer.getName(),
                    chosenBidType,
                    Suit.values()
            );
        }

        commitBid(chosenBidType.instantiate(currentPlayer, null));
        updateCurrentPlayer();
        return null;
    }

    /**
     * Validates and commits the suit for a pending bid.
     *
     * @param suit The suit chosen by the player.
     * @return GameResult or null to continue normal flow.
     */
    private GameResult handleSuitInput(Suit suit) {
        commitBid(pendingBidType.instantiate(currentPlayer, suit));
        this.pendingBidType = null;
        return null;
    }

    /**
     * Processes the choice of a proposer when their proposal is rejected.
     *
     * @param decision The bid type the proposer falls back to.
     * @return GameResult
     */
    private GameResult handleRejectedProposal(BidType decision) {
        removeProposalBid();
        Bid chosenBid = decision.instantiate(currentPlayer, null);
        commitBid(chosenBid);
        return new BiddingCompleted();
    }

    /**
     * Handles the ending of the bidding phase.
     *
     * @return GameResult
     */
    private GameResult handleEndOfBidding() {
        if (currentHighestBidType == BidType.PROPOSAL) {
            return new ProposalRejected(findBid(BidType.PROPOSAL).getPlayer().getName());
        }
        return new BiddingCompleted();
    }

    /**
     * Updates state variables and advances the turn.
     */
    private void commitBid(Bid finalizedBid) {
        this.bids.add(finalizedBid);
        if (currentHighestBidType == null || finalizedBid.getType().compareTo(currentHighestBidType) > 0) {
            currentHighestBidType = finalizedBid.getType();
            currentTrumpSuit = finalizedBid.determineTrump(dealtTrumpSuit);
        }
    }

    /**
     * Advances the turn to the next player in turn order. Skips players who already have a forced bid.
     */
    private void updateCurrentPlayer() {
        List<Player> players = getGame().getPlayers();

        do {
            this.currentPlayer = players.get((players.indexOf(currentPlayer) + 1) % players.size());
            // Keep skipping IF the bidding isn't done AND the player we landed on already has a bid!
        } while (!isBiddingComplete() && bids.stream().anyMatch(bid -> bid.getPlayer().equals(currentPlayer)));
    }

    /**
     * Removes the PROPOSAL bid.
     */
    private void removeProposalBid() {
        Bid proposalBid = findBid(BidType.PROPOSAL);
        bids.remove(proposalBid);
    }

    /**
     * Enforces the hierarchical rules of Whist bidding.
     *
     * @return true if the bid type is legal given current state.
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
        if (chosenBidType.getCategory() != BidCategory.MISERIE) {
            return comparison != 0;
        }
        return true;
    }

    /**
     * Checks if bidding is completed
     *
     * @return true if all players have submitted a bid.
     */
    private boolean isBiddingComplete() {
        return this.bids.size() == getGame().getPlayers().size();
    }

    /**
     *
     * @param bidType the bid type to search for.
     * @return Bid that is linked to the bidType or null if not found
     */
    private Bid findBid(BidType bidType) {
        return bids.stream().filter(b -> b.getType() == bidType).findFirst().orElse(null);
    }

    /**
     * Prepares the Round object with the winning bidder and trump suit.
     */
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
        game.getCurrentRound().startPlayPhase(this.bids, winningBid, this.currentTrumpSuit, firstPlayer);
    }
}