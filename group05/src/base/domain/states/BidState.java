package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.bid.PassBid;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.commands.BidCommand;
import base.domain.commands.ContinueCommand;
import base.domain.commands.GameCommand;
import base.domain.commands.PlaceBidCommand;
import base.domain.commands.SuitCommand;
import base.domain.player.Player;
import base.domain.results.BidTurnResult;
import base.domain.results.BiddingCompleted;
import base.domain.results.GameResult;
import base.domain.results.ProposalRejected;
import base.domain.results.SuitSelectionRequired;
import base.domain.round.Round;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Bidding phase of the Whist game.
 *
 * @author Stan Kestens
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
        this.trumpSuit = null;

        dealCards();
        initializeRound();
    }

    /**
     * Deals 13 cards to each player and sets the trump suit based on the
     * last card dealt to the last player.
     */
    private void dealCards() {
        List<List<Card>> hands = getGame().getDeck().deal();
        List<Player> allPlayers = getGame().getPlayers();
        for (int i = 0; i < allPlayers.size(); i++) {
            allPlayers.get(i).setHand(hands.get(i));
        }
        trumpSuit = allPlayers.getLast().getHand().getLast().suit();
    }

    /**
     * Creates the Round object and applies a points multiplier if the
     * previous round was passed.
     */
    private void initializeRound() {
        WhistGame game = this.getGame();
        int multiplier = 1;
        if (!game.getRounds().isEmpty()) {
            multiplier = game.getCurrentRound().getHighestBid().getType() == BidType.PASS ? 2 : 1;
        }
        game.addRound(new Round(game.getPlayers(), currentPlayer, multiplier));
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
        }

        if (isBiddingComplete())
            return handleEndOfBidding();

        return new BidTurnResult(
                currentPlayer.getName(),
                trumpSuit,
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
            getGame().getDeck().shuffle();
            getGame().getCurrentRound().setHighestBid(findBid(BidType.PASS));
            getGame().getPlayers().forEach(Player::flushHand);
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

        replaceProposalBid(decision);
        this.currentHighestBidType = decision;
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
        }
        updateCurrentPlayer();
    }

    /**
     * Advances currentPlayer to the next player in turn order.
     */
    private void updateCurrentPlayer() {
        List<Player> players = getGame().getPlayers();
        this.currentPlayer = players.get((players.indexOf(currentPlayer) + 1) % players.size());
    }

    /**
     * Replaces a PROPOSAL bid with the proposer's fallback decision.
     *
     * @param chosenBidType the bid type to replace PROPOSAL with.
     */
    private void replaceProposalBid(BidType chosenBidType) {
        Bid proposalBid = findBid(BidType.PROPOSAL);
        int index = bids.indexOf(proposalBid);
        bids.set(index, chosenBidType.instantiate(proposalBid.getPlayer(), null));
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
     * @return true if all players have submitted a bid.
     */
    private boolean isBiddingComplete() {
        return this.bids.size() == getGame().getPlayers().size();
    }

    /**
     * @param bidType the bid type to search for.
     * @return the Bid matching that type, or null.
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

        Round current = game.getCurrentRound();
        current.setCurrentPlayer(firstPlayer);
        current.setHighestBid(findBid(currentHighestBidType));
        current.setBids(this.bids);
        current.setTrumpSuit(trumpSuit);
    }
}