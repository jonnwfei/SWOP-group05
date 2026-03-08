package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.round.Round;
import cli.elements.GameEvent;
import cli.elements.QuestionEvent;
import cli.elements.TextEvent;
import java.util.ArrayList;
import java.util.List;


public class BidState extends State {
    private final List<Bid> bids;
    private BidType currentHighestBidType;
    private Player currentPlayer;
    private Suit trumpSuit;
    private BidType pendingBidType;

    public BidState(WhistGame game) {
        super(game);
        this.bids = new ArrayList<>();
        this.currentHighestBidType = null; // Starts as null!
        this.currentPlayer = game.getCurrentPlayer();
        this.trumpSuit = null;

        dealCards();
    }

    private void dealCards() {
        List<List<Card>> hands = getGame().getDeck().deal();
        List<Player> allPlayers = getGame().getPlayers();
        for(int i = 0; i < allPlayers.size(); i++) {
            allPlayers.get(i).setHand(hands.get(i));
        }

        Player lastPlayer = allPlayers.getLast();
        trumpSuit = lastPlayer.getHand().getLast().suit();    }

    @Override
    public GameEvent executeState(String input) {
        // 2. Handle the "Rejected Proposal" Decision
        // This occurs AFTER everyone has had a turn, and the highest was a PROPOSAL
        if (isBiddingComplete() && currentHighestBidType == BidType.PROPOSAL) {
            return handleRejectedProposal(input);
        }

        // PROCESS INCOMING DATA
        if(input != null && !input.trim().isEmpty()) {

            QuestionEvent errorOrFollowUpPrompt;

            // Route the input based on the current context (Multi-step prompt memory)
            if (this.pendingBidType != null) {
                errorOrFollowUpPrompt = handleSuitInput(input);
            } else {
                errorOrFollowUpPrompt = handleBidInput(input);
            }

            // If a helper caught an error or asked a follow-up question, return it immediately!
            if (errorOrFollowUpPrompt != null) {
                return errorOrFollowUpPrompt;
            }

            // CHECK END CONDITION
            if (isBiddingComplete()) {
                return handleEndOfBidding();
            }
        }

        //GENERATE NEXT PROMPT (First Player or Next Player)
        String promptText;
        if (this.bids.isEmpty()) {
            promptText = buildFirstPlayerPrompt(currentPlayer);
        } else {
            promptText = buildStandardPrompt(currentPlayer);
        }

        return new QuestionEvent(promptText);
    }

    @Override
    public State nextState(){
        if (currentHighestBidType == BidType.PASS) {
            //TODO resetGame: flushHand players, set trump card to null, multiplier...
            return new BidState(getGame());
        }
        return new PlayState(getGame());
    }

    private QuestionEvent handleBidInput(String input) {
        try {
            BidType chosenBidType = parseBidType(input);

            if (!isLegalBidType(chosenBidType)) {
                return new QuestionEvent("Illegal Bid chosen.\nTry again: ");
            }

            if (chosenBidType.getRequiresSuit()) {
                this.pendingBidType = chosenBidType;
                return new QuestionEvent("You chose " + chosenBidType.name() + ".\n\n" +
                        buildSuitOptions() + "Your choice: ");
            }

            Bid finalizedBid = chosenBidType.instantiate(currentPlayer, null);
            commitBid(finalizedBid);
            return null;

        } catch (IllegalArgumentException error) {
            return new QuestionEvent(error.getMessage() + "\nTry again: ");
        }
    }

    private QuestionEvent handleSuitInput(String input) {
        try {
            Suit chosenSuit = parseSuit(input);
            Bid finalizedBid = pendingBidType.instantiate(currentPlayer, chosenSuit);

            commitBid(finalizedBid);
            this.pendingBidType = null;
            return null;

        } catch (IllegalArgumentException error) {
            return new QuestionEvent(error.getMessage() + "\nTry again: ");
        }
    }

    private GameEvent handleRejectedProposal(String input) {
        try {
            int choice = Integer.parseInt(input.trim());
            BidType decision;

            if (choice == 0) decision = BidType.PASS;
            else if (choice == 1) decision = BidType.SOLO_PROPOSAL;
            else return new QuestionEvent("Invalid choice. Choose [0] PASS or [1] SOLO_PROPOSAL: ");
            replaceProposalBid(decision);
            return new TextEvent("\n=== BIDDING COMPLETE ===");
        } catch (Exception e) {
            return new QuestionEvent("Please enter 0 or 1: ");
        }
    }

    private GameEvent handleEndOfBidding() {
        if (currentHighestBidType == BidType.PROPOSAL) {
            Player proposer = findProposalBid().getPlayer();
            int passIdx = BidType.PASS.ordinal();
            int soloPropIdx = BidType.SOLO_PROPOSAL.ordinal();
            return new QuestionEvent("\n" + proposer.getName() + ": No one accepted. [" + passIdx + "] PASS or [" + soloPropIdx + "] SOLO_PROPOSAL?");
        }
        return new TextEvent("\n=== BIDDING COMPLETE ===");
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
        //TODO getGame().getCurrentRound().advanceToNextPlayer();
    }

    private void updateHighestBidType(BidType bidType) {
        if (currentHighestBidType == null || bidType.compareTo(currentHighestBidType) > 0) {
            currentHighestBidType = bidType;
        }
    }

    private void replaceProposalBid(BidType chosenBidType) {
        Bid proposalBid = findProposalBid();
        int index = bids.indexOf(proposalBid);
        bids.set(index, chosenBidType.instantiate(proposalBid.getPlayer(), null));
    }

    private BidType parseBidType(String input) {
        int choiceIndex;
        try {choiceIndex = Integer.parseInt(input.trim());}
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid input! Please enter a number.");
        }

        BidType[] allBids = BidType.values();
        if (choiceIndex < 0 || choiceIndex >= allBids.length) {
            throw new IllegalArgumentException("That number is not on the options.");
        }
        return allBids[choiceIndex];
    }

    private Suit parseSuit(String input) {
        int choiceIndex;
        try {choiceIndex = Integer.parseInt(input.trim());}
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid input! Please enter a number.");
        }

        Suit[] allSuits = Suit.values();
        if (choiceIndex < 0 || choiceIndex >= allSuits.length) {
            throw  new IllegalArgumentException("That number is not on the options.");
        }
        return allSuits[choiceIndex];
    }

    private boolean isLegalBidType(BidType chosenBidType) {
        if(chosenBidType == BidType.PASS) {return true;}
        //If there is no highest bid yet, any bid is legal
        if (currentHighestBidType == null) {return true;}
        if (chosenBidType == BidType.ACCEPTANCE && currentHighestBidType != BidType.PROPOSAL) {return false;}
        if (chosenBidType == BidType.SOLO_PROPOSAL && !isBiddingComplete()) {return false;}
        int comparison = chosenBidType.compareTo(currentHighestBidType);
        if (comparison < 0) {return false;}
        if (comparison == 0 && chosenBidType.getCategory() != BidCategory.MISERIE) {return false;}

        return true;
    }

    private boolean isBiddingComplete() {
        return this.bids.size() == getGame().getPlayers().size();
    }

    private Bid findProposalBid() {
        return bids.stream()
                .filter(b -> b.getType() == BidType.PROPOSAL)
                .findFirst()
                .orElse(null); // Or throw an exception
    }

    private String buildFirstPlayerPrompt(Player player) {
        return "\n=== BIDDING TURN: " + player.getName().toUpperCase() + " ===\n" +
                "Dealt Trump: " + trumpSuit.name() + "\n" +
                player.getFormattedHand() + "\n" +
                "---------------------------------------\n" +
                "Status: You are the first to bid!\n\n" +
                buildBidTypeOptions() + "\n" +
                "Your choice: ";
    }

    private String buildStandardPrompt(Player player) {
        return "\n=== BIDDING TURN: " + player.getName().toUpperCase() + " ===\n" +
                "Dealt Trump: " + trumpSuit.name() + "\n" +
                player.getFormattedHand() + "\n" +
                "---------------------------------------\n" +
                "Current Highest: " + currentHighestBidType.name() + "\n\n" +
                buildBidTypeOptions() + "\n" +
                "Your choice: ";
    }

    private String buildBidTypeOptions() { // Typo fixed!
        StringBuilder options = new StringBuilder("All Options:\n");
        BidType[] allTypes = BidType.values();
        for (int i = 0; i < allTypes.length; i++) {
            BidType currentBid = allTypes[i];
            options.append("   [").append(i).append("] ").append(currentBid.name()).append("\n");
        }
        return options.toString();
    }

    private String buildSuitOptions() {
        StringBuilder options = new StringBuilder("All options:\n");
        Suit[] allSuits = Suit.values();
        for (int i = 0; i < allSuits.length; i++) {
            Suit currentSuit = allSuits[i];
            options.append("   [").append(i).append("] ").append(currentSuit.name()).append("\n");
        }
        return options.toString();
    }
}