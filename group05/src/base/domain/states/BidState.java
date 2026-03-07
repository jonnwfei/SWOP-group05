package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.round.Round;
import cli.elements.GameEvent;
import cli.elements.QuestionEvent;
import cli.elements.TextEvent;
import java.util.ArrayList;
import java.util.List;


public class BidState extends State {
    private final Round currentRound;
    private List<Bid> bids;
    private BidType currentHighestBidType;
    private Player currentPlayer;
    private Suit trumpSuit;
    private BidType pendingBidType;

    public BidState(WhistGame game) {
        super(game);
        this.currentRound = game.getCurrentRound(); //TODO
        this.bids = new ArrayList<>();
        this.currentHighestBidType = null; // Starts as null!
        this.currentPlayer = game.getCurrentPlayer();
        this.trumpSuit = null;
    }

    @Override
    public GameEvent executeState(String input) {

        if(TrumpSuit == null) {
            getGame().getDeck().dealCards(); //TODO
            TrumpSuit = currentRound.getTrumpSuit(); //TODO
        }

        if(input != null && !input.trim().isEmpty()) {

            QuestionEvent errorOrFollowUpPrompt;

            if (this.pendingBidType != null) {
                errorOrFollowUpPrompt = handleSuitInput(input);
            } else {
                errorOrFollowUpPrompt = handleBidInput(input);
            }

            // If a helper caught an error or asked a follow-up question, return it immediately!
            if (errorOrFollowUpPrompt != null) {
                return errorOrFollowUpPrompt;
            }

            if (isBiddingComplete()) {
                //TODO: set active bids and currentPlayer
                return new TextEvent("\n=== BIDDING COMPLETE ===");
            }
        }

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
        return new PlayState(getGame());
    }

    private void updateCurrentPlayer(List<Player> players) {
        int index = players.indexOf(currentPlayer);
        if (index == -1) {throw new IllegalArgumentException("currentPlayer not found in list of players");}
        int newIndex = (index + 1) % players.size();
        this.currentPlayer = players.get(newIndex);
        getGame().setCurrentPlayer(currentPlayer); //TODO
    }

    private void updateHighestBidType(BidType bidType) {
        if (currentHighestBidType == null || bidType.compareTo(currentHighestBidType) > 0) {
            currentHighestBidType = bidType;
        }
    }

    private String buildFirstPlayerPrompt(Player player) {
        return "\n=== BIDDING TURN: " + player.getName().toUpperCase() + " ===\n" +
                "Dealt Trump: " + TrumpSuit.name() + "\n" +
                // TODO: "Your Hand: " + player.getFormattedHand() + "\n"
                "---------------------------------------\n" +
                "Status: You are the first to bid!\n\n" +
                buildBidTypeOptions() + "\n" +
                "Your choice: ";
    }

    private String buildStandardPrompt(Player player) {
        return "\n=== BIDDING TURN: " + player.getName().toUpperCase() + " ===\n" +
                "Dealt Trump: " + TrumpSuit.name() + "\n" +
                // TODO: "Your Hand: " + formatHand(player.getHand()) + "\n"
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

    private BidType determineBid(int index) {
        BidType[] allBids = BidType.values();
        if (index < 0 || index >= allBids.length) {return null;}
        return allBids[index];
    }

    private Suit determineSuit(int index) {
        Suit[] allSuits = Suit.values();
        if (index < 0 || index >= allSuits.length) {return null;}
        return allSuits[index];
    }

    private BidType parseBidType(String input) {
        int choiceIndex;
        try {choiceIndex = Integer.parseInt(input.trim());}
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid input! Please enter a number.");
        }

        BidType selectedType = determineBid(choiceIndex);
        if (selectedType == null) {
            throw  new IllegalArgumentException("That number is not on the options.");
        }
        return selectedType;
    }

    private Suit parseSuit(String input) {
        int choiceIndex;
        try {choiceIndex = Integer.parseInt(input.trim());}
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid input! Please enter a number.");
        }

        Suit selectedSuit = determineSuit(choiceIndex);
        if (selectedSuit == null) {
            throw  new IllegalArgumentException("That number is not on the options.");
        }
        return selectedSuit;
    }

    private boolean isLegalBidType(BidType chosenBidType) {
        if (chosenBidType == BidType.PASS) {return true;}
        //If there is no highest bid yet, any bid is legal
        if (currentHighestBidType == null) {return true;}

        int comparison = chosenBidType.compareTo(currentHighestBidType);
        if (comparison < 0) {return false;}
        if (comparison == 0 && chosenBidType.getCategory() != BidCategory.MISERIE) {return false;}

        return true;
    }

    private QuestionEvent handleSuitInput(String input) {
        try {
            Suit chosenSuit = parseSuit(input);
            Bid finalizedBid = pendingBidType.instantiate(currentPlayer, chosenSuit);

            this.bids.add(finalizedBid);
            this.pendingBidType = null;

            updateHighestBidType(finalizedBid.getType());
            updateCurrentPlayer(getGame().getPlayers());

            return null;

        } catch (IllegalArgumentException error) {
            return new QuestionEvent(error.getMessage() + "\nTry again: ");
        }
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
            this.bids.add(finalizedBid);

            updateHighestBidType(finalizedBid.getType());
            updateCurrentPlayer(getGame().getPlayers());

            return null; // Null means success

        } catch (IllegalArgumentException error) {
            return new QuestionEvent(error.getMessage() + "\nTry again: ");
        }
    }

    private boolean isBiddingComplete() {
        return this.bids.size() == getGame().getPlayers().size();
    }

}

