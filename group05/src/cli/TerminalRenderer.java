package cli;

import base.domain.events.*;
import base.domain.events.bidevents.*;
import base.domain.events.countEvents.*;
import base.domain.events.playevents.*;

import java.util.List;

public class TerminalRenderer {
    public void render(GameEvent event) {
        switch (event) {
            case BidTurnEvent e -> renderBidTurnEvent(e);
            case RejectedProposalEvent e -> renderRejectedProposalEvent(e);
            case SuitPromptEvent e -> renderSuitPromptEvent(e);
            case EndOfRoundEvent e -> renderEndOfRoundEvent(e);
            case EndOfTrickEvent e -> renderEndOfTrickEvent(e);
            case InitiateTurnEvent e -> renderInitiateTurnEvent(e);
            case PickCardEvent e -> renderPickCardEvent(e);
            case ErrorEvent e -> renderErrorEvent(e);
            case GetSuitEvent e -> renderGetSuitEvent();
            case MiserieWinnerEvent e -> renderMiserieWinnerEvent(e);
            case PlayersInBidEvent e -> renderPlayersInBidEvent(e);
            case ScoreBoardEvent e -> renderScoreBoardEvent(e);
            case TrickWonEvent e -> renderTrickWonEvent();
            case WelcomeCountEvent e -> renderWelcomeCountEvent();

            default -> System.out.println("[WARNING] Unknown event type!");  }
    }

    private void renderBidTurnEvent(BidTurnEvent event) {
        System.out.println("\n=== BIDDING TURN: " + event.playerName().toUpperCase() + " ===");
        System.out.println("Dealt Trump: " + event.dealtTrump().toString());
        if(event.currentHighestBidType() == null) {
            System.out.println("You are the first to bid!");
        }
        else System.out.println("Current Highest: " + event.currentHighestBidType());
        System.out.println("All Options:");
        for (int i = 0; i < event.bidTypes().length; i++) {
            System.out.println("   [" + i + "] " + event.bidTypes()[i].name());
        }
        System.out.print("Your choice: ");
    }

    private void renderRejectedProposalEvent(RejectedProposalEvent event) {
        System.out.println(event.proposerName() + ": no one accepted your proposal.");
        System.out.println("Choose [0] PASS or [1] SOLO_PROPOSAL:");
    }

    private void renderSuitPromptEvent(SuitPromptEvent event) {
        System.out.println("\n" + event.playerName() + ", you chose " + event.pendingType().name() + ".");
        System.out.println("All Options:");
        for (int i = 0; i < event.suits().length; i++) {
            System.out.println("   [" + i + "] " + event.suits()[i].name());
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
        System.out.println("\n============== NEXT TRICK ==============");
        }

    private void renderInitiateTurnEvent (InitiateTurnEvent event) {
        System.out.println("============== Pass the turn to " + event.playerName() + " ==============");
        System.out.println("\nPress ANY BUTTON to reveal your hand...");
    }

    private void renderPickCardEvent(PickCardEvent event) {
        System.out.println("\n-------------- CARDS ON TABLE ---------------");

        if(event.cardsOnTable().isEmpty()) {
            System.out.println("(No cards played yet)");
        } else {
            for(String cardStr : event.cardsOnTable()) {
                System.out.println("- " + cardStr);
            }
        }

        // The 'if (false)' is replaced with our pure boolean data!
        if (event.isOpenMiserie()) {
            System.out.println("\n--- EXPOSED HAND (OPEN_MISERIE: " + event.exposedPlayerName() + ") ---");
            System.out.println(event.formattedExposedHand());
        }
        System.out.println("---------------------------------------------");

        // Fixed the math concatenation bug by putting the math in parentheses!
        System.out.println("Trick: " + event.trickNumber() + " | " + event.currentPlayerName() + "'s turn.");

        System.out.println("[0] Show last played Trick.");
        System.out.println("Your hand: ");

        // Fixed the <= out of bounds bug.
        // Notice we start formatting at [1] because [0] is reserved for "Show last trick"
        List<String> hand = event.currentPlayerHand();
        for (int i = 0; i < hand.size(); i++) {
            System.out.println("   [" + (i + 1) + "] " + hand.get(i));
        }

        System.out.print("Choose Card via index: ");
    }

    private void renderErrorEvent(ErrorEvent event) {
        System.out.println("Please give a number between " + event.lowerBound() + " and " + event.upperBound());
    }

    private void renderGetSuitEvent() {
        System.out.println("What Suit is the trump suit?");
        System.out.println("(1) Hearts (2) Clubs (3) Diamonds (4) Spades");
        System.out.print("Your choice: ");
    }

    private void renderMiserieWinnerEvent(MiserieWinnerEvent event) {
        System.out.println("Which players won their bid? (Got 0 tricks): ");
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

    private void renderScoreBoardEvent(ScoreBoardEvent event) {
        System.out.println("============== SCORES ==============");
        List<String> names = event.playerNames();
        List<Integer> scores = event.scores();

        for (int i = 0; i < names.size(); i++) {
            System.out.println(names.get(i) + ": " + scores.get(i) + " points");
        }
        System.out.println("====================================");
        System.out.println("Do you want to:");
        System.out.println("(1) Simulate another round\n(2) Go back to the main menu");
        System.out.print("Your choice: ");
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
}
