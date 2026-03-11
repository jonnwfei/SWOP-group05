package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.actions.TextAction;
import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.bid.PassBid;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.events.*;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.events.bidevents.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Bidding phase of a Whist game.
 * This state handles initializing the new round, dealing cards, taking bids sequentially from players,
 * enforcing legal bid rules, and transitioning the game to the Play state.
 */
public class BidState extends State {
    private final List<Bid> bids;
    private BidType currentHighestBidType;
    private Player currentPlayer;
    private Suit trumpSuit;
    private BidType pendingBidType;

    /**
     * Constructs a new BidState.
     * Automatically deals cards and initializes the round with multiplier upon creation.
     *
     * @param game The current WhistGame instance.
     */
    public BidState(WhistGame game) {
        super(game);
        this.bids = new ArrayList<>();
        this.currentHighestBidType = null; // Starts as null!
        Player dealerPlayer = game.getDealerPlayer();
        int dealerIdx = game.getPlayers().indexOf(dealerPlayer);
        this.currentPlayer = game.getPlayers().get((dealerIdx + 1)% game.getPlayers().size());
        this.trumpSuit = null;

        dealCards();
        initializeRound();
    }

    /**
     * Deals 13 cards to each of the 4 players.
     * The suit of the last card dealt to the last player determines the initial trump suit.
     */
    private void dealCards() {
        List<List<Card>> hands = getGame().getDeck().deal();
        List<Player> allPlayers = getGame().getPlayers();
        for(int i = 0; i < allPlayers.size(); i++) {
            allPlayers.get(i).setHand(hands.get(i));
        }

        Player lastPlayer = allPlayers.getLast();
        trumpSuit = lastPlayer.getHand().getLast().suit();    }

    /**
     * Initializes the round with the appropriate current player.
     * Applies a double-point multiplier if the previous round ended with all players passing.
     */
    private void initializeRound() {
        WhistGame game = this.getGame();
        List<Player> players = game.getPlayers();

        int multiplier;
        if (getGame().getRounds().isEmpty()){
            multiplier = 1;
        }
        else{
            multiplier = game.getCurrentRound().getHighestBid().getType() == BidType.PASS ? 2 : 1;
        }
        Round newRound = new Round(players, currentPlayer, multiplier);
        game.addRound(newRound);
    }

    /**
     * Processes pure Domain Actions and advances the bidding state machine.
     * doesn't expect TextAction
     */
    @Override
    public GameEvent executeState(GameAction action) {

        // 2. Route Number Actions based on context
        if (action instanceof NumberAction(int choice)) {

            // CONTEXT A: Are we resolving a rejected proposal?
            if (isBiddingComplete() && currentHighestBidType == BidType.PROPOSAL) {
                return handleRejectedProposal(choice);
            }

            // CONTEXT B: Are we waiting for a suit selection?
            if (this.pendingBidType != null) {
                GameEvent error = handleSuitInput(choice);
                if (error != null) return error;
            }

            // CONTEXT C: Normal Bid Selection
            else {
                GameEvent followUpOrError = handleBidInput(choice);
                if (followUpOrError != null) return followUpOrError; // SuitPromptEvent or ErrorEvent
            }
        }

        // 3. Process Player Bots (Fast-forwards if the current player doesn't require UI input)
        while(!currentPlayer.getRequiresConfirmation() && !isBiddingComplete()) {
            Bid finalizedBid = new PassBid(currentPlayer);
            commitBid(finalizedBid);
        }

        // 4. Check End Conditions
        if (isBiddingComplete()) {
            return handleEndOfBidding();
        }

        // 5. Generate Next Turn Prompt (Null highest bid gracefully handles the first player)
        return new BidTurnEvent(currentPlayer.getName(), trumpSuit, currentHighestBidType, BidType.values());
    }

    @Override
    public State nextState(){
        if (currentHighestBidType == BidType.PASS) {
            getGame().getDeck().shuffle();
            getGame().getCurrentRound().setHighestBid(findBid(currentHighestBidType));
            getGame().getPlayers().forEach(Player::flushHand);
            return new BidState(getGame());
        }
        setRoundReadyForPlayState();
        return new PlayState(getGame());
    }

    /**
     * @return An ErrorEvent or SuitPromptEvent if action is required, or null if successful.
     */
    private GameEvent handleBidInput(int choice) {
        BidType[] allBids = BidType.values();
        if (choice < 1 || choice > allBids.length) {
            return new ErrorEvent(1, allBids.length);
        }

        BidType chosenBidType = allBids[choice-1];

        if (!isLegalBidType(chosenBidType)) {
            return new ErrorEvent(1, allBids.length);//illegalMoveEvent
        }

        if (chosenBidType.getRequiresSuit()) {
            this.pendingBidType = chosenBidType;
            return new SuitPromptEvent(currentPlayer.getName(), chosenBidType, Suit.values());
        }

        Bid finalizedBid = chosenBidType.instantiate(currentPlayer, null);
        commitBid(finalizedBid);
        return null;
    }

    /**
     * @return An ErrorEvent if illegal, or null if successful.
     */
    private GameEvent handleSuitInput(int choice) {
        Suit[] allSuits = Suit.values();
        if (choice < 1 || choice > allSuits.length) {
            return new ErrorEvent(1, allSuits.length);
        }

        Suit chosenSuit = allSuits[choice];
        Bid finalizedBid = pendingBidType.instantiate(currentPlayer, chosenSuit);

        commitBid(finalizedBid);
        this.pendingBidType = null;
        return null;
    }

    private GameEvent handleRejectedProposal(int choice) {
        BidType decision;

        if (choice == 0) decision = BidType.PASS;
        else if (choice == 1) decision = BidType.SOLO_PROPOSAL;
        else return new ErrorEvent(0, 1);

        replaceProposalBid(decision);
        this.currentHighestBidType = decision;
        return new BiddingCompleteEvent();
    }

    private GameEvent handleEndOfBidding() {
        if (currentHighestBidType == BidType.PROPOSAL) {
            Player proposer = findBid(BidType.PROPOSAL).getPlayer();
            return new RejectedProposalEvent(proposer.getName());
        }
        return new BiddingCompleteEvent(); //EndStateEvent
    }

    private void commitBid(Bid finalizedBid) {
        this.bids.add(finalizedBid);
        updateHighestBidType(finalizedBid.getType());
        updateCurrentPlayer(getGame().getPlayers());
    }

    private void updateCurrentPlayer(List<Player> players) {
        int index = players.indexOf(currentPlayer);
        if (index == -1) {throw new IllegalArgumentException("currentPlayer not found in list of players");}
        int newIndex = (index + 1) % players.size();
        this.currentPlayer = players.get(newIndex);
    }

    private void updateHighestBidType(BidType bidType) {
        if (currentHighestBidType == null || bidType.compareTo(currentHighestBidType) > 0) {
            currentHighestBidType = bidType;
        }
    }

    private void replaceProposalBid(BidType chosenBidType) {
        Bid proposalBid = findBid(BidType.PROPOSAL);
        int index = bids.indexOf(proposalBid);
        bids.set(index, chosenBidType.instantiate(proposalBid.getPlayer(), null));
    }

    private boolean isLegalBidType(BidType chosenBidType) {
        if(chosenBidType == BidType.PASS) {return true;}
        if (currentHighestBidType == null) {return true;}
        if (chosenBidType == BidType.ACCEPTANCE && currentHighestBidType != BidType.PROPOSAL) {return false;}
        if (chosenBidType == BidType.SOLO_PROPOSAL && !isBiddingComplete()) {return false;}

        int comparison = chosenBidType.compareTo(currentHighestBidType);
        if (comparison < 0) {return false;}
        if (chosenBidType.getCategory() != BidCategory.MISERIE) {
            return comparison != 0;
        }
        return true;
    }

    private boolean isBiddingComplete() {
        return this.bids.size() == getGame().getPlayers().size();
    }

    private Bid findBid(BidType bidType) {
        return bids.stream()
                .filter(b -> b.getType() == bidType)
                .findFirst()
                .orElse(null);
    }

    private void setRoundReadyForPlayState() {
        WhistGame game = this.getGame();
        List<Player> players = game.getPlayers();

        Player firstPlayer = players.get((players.indexOf(game.getDealerPlayer()) + 1) % 4);

        if (this.currentHighestBidType.getCategory() == BidCategory.ABONDANCE ||
                this.currentHighestBidType.getCategory() == BidCategory.SOLO) {
            firstPlayer = findBid(currentHighestBidType).getPlayer();
        }
        game.getCurrentRound().setCurrentPlayer(firstPlayer);
        game.getCurrentRound().setHighestBid(findBid(currentHighestBidType));
        game.getCurrentRound().setBids(this.bids);
        game.getCurrentRound().setTrumpSuit(trumpSuit);
    }
}