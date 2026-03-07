package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.player.Player;
import cli.elements.GameEvent;
import cli.elements.QuestionEvent;

import java.util.ArrayList;
import java.util.List;


public class BidState extends State {
    private List<Bid> bids;
    private int promptCount;
    private int amountBids;
    private Bid currentHighestBid;
    private Player currentPlayer;

    public BidState(WhistGame game) {
        super(game);
        this.bids = new ArrayList<>();
        this.promptCount = 0;
        this.amountBids = 0;
        this.currentHighestBid = null; // Starts as null!
        this.currentPlayer = game.getCurrentPlayer();
    }

    @Override
    public GameEvent executeState(String input) {

        inputHandler(input);

        String promptText = "";
        if (promptCount == 0) {
            promptText = buildFirstPlayerPrompt(currentPlayer);
        }
        else if (amountBids < getGame().getPlayers().size()){
            promptText = buildStandardPrompt(currentPlayer, currentHighestBid);
        }
        else {promptText = buildEndBiddingPrompt()}

        promptCount++;
        amountBids++;// We have now asked a question
        return new QuestionEvent(promptText);
    }

    @Override
    public State nextState(){
        return new PlayState(getGame());
    }


    private Player updateCurrentPlayer(List<Player> players) {
        int index = players.indexOf(currentPlayer);
        if (index == -1) {throw new IllegalArgumentException("currentPlayer not found in list of players");}
        int newIndex = (index + 1) % players.size();
        this.currentPlayer = players.get(newIndex);
        return currentPlayer;
    }

    private String buildFirstPlayerPrompt(Player player) {
        return "=== BIDDING TURN: " + player.getName() + " ===\n" +
                "you are the first to bid!\n" +
                 buildFullMenu() + // Show all options
                "Your choice: ";
    }

    private String buildStandardPrompt(Player player, Bid currentHighest) {
        return "=== BIDDING TURN: " + player.getName() + " ===\n" +
                "current Highest:" + currentHighestBid.getType().name() +
                buildFullMenu() + // Show all options
                "Your choice: ";
    }

    private String buildFullMenu() {
        String options = "";
        BidType[] allTypes = BidType.values();
        for (int i = 0; i < allTypes.length; i++) {
            BidType currentBid = allTypes[i];
            options = options.concat("   [" + String.valueOf(i) + "]" + currentBid.name() + "\n");
        }
        return "All Options:\n" + options;
    }

    private BidType determineBid(int index) {
        BidType[] allBids = BidType.values();
        if (index < 0 || index >= allBids.length) {
            return null;
        }
        return allBids[index];
    }

    private void inputHandler(String input) {
        if (input != null && !input.trim().isEmpty()) {
            int choiceIndex = -1;

            try {
                choiceIndex = Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                return new QuestionEvent("Invalid input! Please enter a number.\nTry again: ");
            }


            BidType selectedType = determineBid(choiceIndex);

            if (selectedType == null) {
                return new QuestionEvent("That number is not on the options.\nTry again: ");
            }

            if

        }
    }
}

