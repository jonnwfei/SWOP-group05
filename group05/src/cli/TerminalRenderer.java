package cli;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import cli.events.IOEvent;
import cli.events.BidEvents.*;
import cli.events.CountEvents.*;
import cli.events.menu.*;

import java.util.List;

public class TerminalRenderer {

    public void render(IOEvent event) {
        switch (event) {
            // --- bid state ---
            case BidTurnIOEvent e              -> renderBidTurnEvent(e);
            case SuitSelectionIOEvent ignored  -> renderSuitSelectionEvent();
            case ProposalRejectedIOEvent e     -> renderRejectedProposalEvent(e);
            case BiddingCompletedIOEvent ignored -> renderBiddingCompleteEvent();
            // --- count state ---
            case BidSelectionIOEvent e         -> renderBidSelectionEvent(e);
            case PlayerSelectionIOEvent e      -> renderPlayerSelectionEvent(e);
            case TrickInputIOEvent ignored     -> renderTrickInputEvent();
            case ScoreBoardIOEvent e           -> renderScoreBoardEvent(e);
            case SaveDescriptionIOEvent ignored -> renderSaveDescriptionEvent();
            // --- menu ---
            case WelcomeMenuIOEvent ignored    -> renderWelcomeMenuEvent();
            case AmountOfBotsIOEvent ignored   -> renderAmountOfBotsEvent();
            case PlayerNameIOEvent e           -> renderPlayerNameEvent(e);
            case BotStrategyIOEvent e          -> renderBotStrategyEvent(e);
            case PrintNamesIOEvent e           -> renderPrintNamesEvent(e);
            default -> throw new IllegalStateException("Unhandled IOEvent: " + event);
        }
    }

    // --- bid state ---

    private void renderBidTurnEvent(BidTurnIOEvent event) {
        System.out.println("\n=== BIDDING TURN: " + event.data().playerName().toUpperCase() + " ===");
        System.out.println("Dealt Trump: " + event.data().trumpSuit());
        System.out.println("Your hand: ");
        System.out.println(event.data().hand());
        if (event.data().currentHighestBid() == null) {
            System.out.println("You are the first to bid!");
        } else {
            System.out.println("Current Highest: " + event.data().currentHighestBid());
        }
        System.out.println("All Options:");
        BidType[] bids = event.data().availableBids();
        for (int i = 0; i < bids.length; i++) {
            System.out.println("   [" + (i + 1) + "] " + bids[i].name());
        }
        System.out.print("Your choice: ");
    }

    private void renderSuitSelectionEvent() {
        System.out.println("Choose a trump suit:");
        Suit[] suits = Suit.values();
        for (int i = 0; i < suits.length; i++) {
            System.out.println("   [" + (i + 1) + "] " + suits[i].name());
        }
        System.out.print("Your choice: ");
    }

    private void renderRejectedProposalEvent(ProposalRejectedIOEvent event) {
        System.out.println(event.data().playerName() + ": no one accepted your proposal.");
        System.out.println("Choose [1] PASS or [2] SOLO_PROPOSAL:");
        System.out.print("Your choice: ");
    }

    private void renderBiddingCompleteEvent() {
        System.out.println("-----BIDDING COMPLETE-----");
    }

    // --- count state ---

    private void renderBidSelectionEvent(BidSelectionIOEvent event) {
        System.out.println("===== WHICH ROUND WAS PLAYED? =====");
        BidType[] types = event.bidTypes();
        for (int i = 0; i < types.length; i++) {
            System.out.println("   [" + (i + 1) + "] " + types[i].name());
        }
        System.out.print("Your choice: ");
    }

    private void renderPlayerSelectionEvent(PlayerSelectionIOEvent event) {
        System.out.println(event.multi() ? "Select all players involved (comma-separated):" : "Select the main bidder:");
        List<String> names = event.players()
                .stream()
                .map(Player::getName)
                .toList();
        for (int i = 0; i < names.size(); i++) {
            System.out.println("   [" + (i + 1) + "] " + names.get(i));
        }
        System.out.print("Your choice: ");
    }

    private void renderTrickInputEvent() {
        System.out.println("How many tricks did the player(s) win?");
        System.out.print("Your choice: ");
    }

    private void renderScoreBoardEvent(ScoreBoardIOEvent event) {
        System.out.println("============== SCORES ==============");
        List<String> names = event.playerNames();
        List<Integer> scores = event.scores();
        for (int i = 0; i < names.size(); i++) {
            System.out.println(names.get(i) + ": " + scores.get(i) + " points");
        }
        System.out.println("====================================");
        System.out.println("(1) Simulate another round");
        System.out.println("(2) Go back to the main menu");
        System.out.println("(3) Save this session");
        System.out.print("Your choice: ");
    }

    private void renderSaveDescriptionEvent() {
        System.out.print("Give a description for this count save: ");
    }

    // --- menu ---

    private void renderWelcomeMenuEvent() {
        System.out.println("======== WELCOME TO WHIST =====");
        System.out.println("(1) Play a game?");
        System.out.println("(2) Count the scores for a game?");
        System.out.println("(3) Resume a saved game/count?");
        System.out.print("Your choice: ");
    }

    private void renderAmountOfBotsEvent() {
        System.out.println("How many bots will be playing? (0-3):");
        System.out.print("Your choice: ");
    }

    private void renderBotStrategyEvent(BotStrategyIOEvent event) {
        System.out.println("Which strategy should bot " + event.botIndex() + " use?");
        System.out.println("(1) High Bot\n(2) Low Bot");
        System.out.print("Your choice: ");
    }

    private void renderPlayerNameEvent(PlayerNameIOEvent event) {
        System.out.print("Give the name of player " + event.playerIndex() + ": ");
    }

    private void renderPrintNamesEvent(PrintNamesIOEvent event) {
        System.out.println("Players in this game:");
        List<String> names = event.playerNames();
        for (int i = 0; i < names.size(); i++) {
            System.out.println((i + 1) + ". " + names.get(i));
        }
    }
}