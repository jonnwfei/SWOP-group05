package cli;

import base.domain.card.Card;
import base.domain.events.*;
import base.domain.events.bidevents.*;
import base.domain.events.countEvents.*;
import base.domain.events.errorEvents.NumberErrorEvent;
import base.domain.events.errorEvents.NumberListErrorEvent;
import base.domain.events.menuEvents.*;
import base.domain.events.playevents.*;
import base.domain.trick.Turn;
import cli.events.IOEvent;
import cli.events.InitializeGameIOEvent;

import java.util.List;

/**
 * Responsible for translating Domain the GameEvents objects into visual
 * representations in the terminal.
 *
 * @author Tommy Wu
 * @since 10/03/2026
 */
public class TerminalRenderer {

    /**
     * Dispatches the given event to its specific rendering method.
     * * @param event the GameEvent it received from the Domain Layer
     */
    public void render(IOEvent event) {
        switch (event) {
            case InitializeGameIOEvent ignored -> initializeGameEvent();
            default -> throw new IllegalStateException("Unexpected value: " + event);
        }
    }

    private void initializeGameEvent() {

    }

    // --- ERROR RENDERING ---

    private void renderNumberListErrorEvent(NumberListErrorEvent e) {
        System.out.print("Give valid input! ");
    }

    private void renderNumberErrorEvent(NumberErrorEvent e) {
        System.out.println(e.errorMessage());
    }

    private void renderErrorEvent(ErrorEvent event) {
        System.out.println("Please give a number between " + event.lowerBound() + " and " + event.upperBound());
    }

    // --- GAMEPLAY RENDERING ---

    /**
     * Renders the bidding interface, including the player's current hand
     * and the available Whist contract options.
     */
    private void renderBidTurnEvent(BidTurnEvent event) {
        System.out.println("\n=== BIDDING TURN: " + event.playerName().toUpperCase() + " ===");
        System.out.println("Dealt Trump: " + event.dealtTrump().toString());
        System.out.println("Your hand: ");
        System.out.println(event.playerHand());
        if(event.currentHighestBidType() == null) {
            System.out.println("You are the first to bid!");
        }
        else System.out.println("Current Highest: " + event.currentHighestBidType());
        System.out.println("All Options:");
        for (int i = 0; i < event.bidTypes().length; i++) {
            System.out.println("   [" + (i+1) + "] " + event.bidTypes()[i].name());
        }
        System.out.print("Your choice: ");
    }

    /**
     * Renders the card-picking interface.
     * <p>Handles special visibility rules, such as revealing an exposed hand
     * during an 'Open Miserie' contract.</p>
     */
    private void renderPickCardEvent(PickCardEvent event) {
        System.out.println("\n-------------- CARDS ON TABLE ---------------");

        if(event.cardsOnTable().isEmpty()) {
            System.out.println("(No cards played yet)");
        } else {
            for(Card card : event.cardsOnTable()) {
                System.out.println("- " + card.toString());
            }
        }

        if (event.isOpenMiserie()) {
            for (int i = 0; i < event.exposedPlayerName().size(); i++) {
                System.out.println("\n--- EXPOSED HAND (OPEN_MISERIE: "
                        + event.exposedPlayerName().get(i) + ") ---");
                System.out.println(event.formattedExposedHand().get(i));
            }
        }
        System.out.println("---------------------------------------------");

        System.out.println("Trick: " + event.trickNumber() + " | " + event.currentPlayerName() + "'s turn.");
        System.out.println("[0] Show last played Trick.");
        System.out.println("Your hand: ");

        List<Card> hand = event.currentPlayerHand();
        for (int i = 0; i < hand.size(); i++) {
            System.out.println("   [" + (i + 1) + "] " + hand.get(i));
        }

        System.out.print("Choose Card via index: ");
    }

    // --- SCORE & STATE RENDERING ---

    /**
     * Displays the current scoreboard and provides navigation options.
     */
    private void renderScoreBoardEvent(ScoreBoardEvent event) {
        System.out.println("============== SCORES ==============");
        List<String> names = event.playerNames();
        List<Integer> scores = event.scores();

        for (int i = 0; i < names.size(); i++) {
            System.out.println(names.get(i) + ": " + scores.get(i) + " points");
        }
        System.out.println("====================================");
        System.out.println("Do you want to:");
        System.out.println("(1) Simulate another round\n(2) Go back to the main menu\n(3) Save this session");
        System.out.print("Your choice: ");
    }

    private void renderEndOfTurnEvent(EndOfTurnEvent event) {
        System.out.println(event.playerName() + " played " + event.card().toString());
    }

    private void renderEndOfCountStateEvent() {
    }

    private void renderScoreBoardCompleteEvent(ScoreBoardCompleteEvent e) {
        System.out.println("\nLoading next screen...");
    }

    private void renderBiddingCompleteEvent() {
        System.out.println("-----BIDDING COMPLETE-----");
    }

    private void renderRejectedProposalEvent(RejectedProposalEvent event) {
        System.out.println(event.proposerName() + ": no one accepted your proposal.");
        System.out.println("Choose [1] PASS or [2] SOLO_PROPOSAL:");
    }

    private void renderSuitPromptEvent(SuitPromptEvent event) {
        System.out.println("\n" + event.playerName() + ", you chose " + event.pendingType().name() + ".");
        System.out.println("All Options:");
        for (int i = 0; i < event.suits().length; i++) {
            System.out.println("   [" + (i+1) + "] " + event.suits()[i].name());
        }
        System.out.print("Your choice: ");
    }

    private void renderEndOfRoundEvent (EndOfRoundEvent event){
        System.out.println(event.playerName() + " played " + event.card().toString());
        System.out.println("\n============== ROUND OVER ==============");
        System.out.println("Calculating final scores...");
    }

    private void renderEndOfTrickEvent(EndOfTrickEvent event) {
        System.out.println(event.playerName() + " played " + event.card().toString());
        System.out.println(event.winnerName() + " won the trick!");
        System.out.println("\n============== NEXT TRICK ==============");
    }

    private void renderInitiateTurnEvent (InitiateTurnEvent event) {
        System.out.println("============== Pass the turn to " + event.playerName() + " ==============");
        System.out.println("\nPress ENTER to reveal your hand...");
    }

    private void renderGetSuitEvent() {
        System.out.println("What Suit is the trump suit?");
        System.out.println("(1) Hearts (2) Clubs (3) Diamonds (4) Spades");
        System.out.print("Your choice: ");
    }

    private void renderMiserieWinnerEvent(MiserieWinnerEvent event) {
        System.out.println("Which players got 0 tricks (-1 if none):  ");
        System.out.println("Players in this game:");
        List<String> names = event.playerNames();
        for (int i = 0; i < names.size(); i++) {
            System.out.println((i + 1) + ". " + names.get(i));
        }
        System.out.print("Your choice (comma-separated): ");
    }

    private void renderPlayersInBidEvent(PlayersInBidEvent event) {
        System.out.println("Which player numbers played this bid?");
        System.out.println("Players in this game:");
        List<String> names = event.playerNames();
        for (int i = 0; i < names.size(); i++) {
            System.out.println((i + 1) + ". " + names.get(i));
        }
        System.out.print("Your choice (comma-separated): ");
    }

    private void renderTrickWonEvent() {
        System.out.println("How many tricks did the player(s) win?");
        System.out.print("Your choice: ");
    }

    private void renderWelcomeCountEvent() {
        System.out.println("===== WELCOME TO THE COUNT ====");
        System.out.println(" WHICH ROUND WAS PLAYED?");
        System.out.println("Proposal:");
        System.out.println("(1) Alone    (2) With Partner");
        System.out.println("Abondance:");
        System.out.println("(3) 9   (4) 10   (5) 11   (6) 12");
        System.out.println("Miserie:");
        System.out.println("(7) Normal       (8) Open");
        System.out.println("Solo:");
        System.out.println("(9) Normal       (10) Solo Slim");
        System.out.print("Your choice: ");
    }

    private void renderAmountOfBotsEvent() {
        System.out.println("How many bots will be playing? (0-3):");
        System.out.print("Your choice: ");
    }

    private void renderBotStrategyEvent(BotStrategyEvent event) {
        System.out.println("Which strategy should bot " + event.botIndex() + " use?");
        System.out.println("(1) High Bot\n(2) Low Bot");
        System.out.print("Your choice: ");
    }

    private void renderPlayerNameEvent(PlayerNameEvent event) {
        System.out.print("Give the name of player " + event.playerIndex() + ": ");
    }

    private void renderPrintNamesEvent(PrintNamesEvent event) {
        System.out.println("Players in this game:");
        List<String> names = event.playerNames();
        for (int i = 0; i < names.size(); i++) {
            System.out.println((i + 1) + ". " + names.get(i));
        }
    }

    private void renderWelcomeMenuEvent() {
        System.out.println("======== WELCOME TO WHIST =====");
        System.out.println("Do you want to:");
        System.out.println("(1) Play a game?");
        System.out.println("(2) Count the scores for a game?");
        System.out.println("(3) Resume a saved game/count?");
        System.out.print("Your choice: ");
    }

    private void renderResumeSaveEvent(ResumeSaveEvent event) {
        System.out.println("Choose a save to resume:");
        for (int i = 0; i < event.descriptions().size(); i++) {
            System.out.println("(" + (i + 1) + ") " + event.descriptions().get(i));
        }
        System.out.print("Your choice: ");
    }

    private void renderSaveDescriptionEvent(SaveDescriptionEvent event) {
        System.out.print("Give a description for this " + event.contextLabel() + " save: ");
    }

    private void renderLastTrickEvent(LastTrickEvent event) {
        System.out.println("-------------- LAST PLAYED TRICK ---------------");
        for(Turn turn : event.trick().getTurns()) {
            System.out.println("- " + turn.toString());
        }
        System.out.println("------------------------------------------------");
    }

    private void renderPlayAgainPromptEvent(PlayAgainPromptEvent event) {
        System.out.println("Do you want to:");
        System.out.println("(1) Play another round");
        System.out.println("(2) Quit to main menu");
        System.out.print("Your choice: ");
    }
}