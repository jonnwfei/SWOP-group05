package cli;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.results.PlayCardResult;
import base.domain.trick.Turn;
import cli.events.IOEvent;
import cli.events.MessageIOEvent;

import static cli.events.BidEvents.*;
import static cli.events.CountEvents.*;
import static cli.events.MenuEvents.*;
import static cli.events.PlayEvents.*;

import java.util.List;

public class TerminalRenderer {
    public void render(IOEvent event) {
        switch (event) {
            // --- play state ---
            case ConfirmationIOEvent e -> renderConfigEvent(e);
            case PlayCardIOEvent e -> renderPlayCardEvent(e);
            case EndOfTurnIOEvent e -> renderEndOfTurnEvent(e);
            case EndOfTrickIOEvent e -> renderEndOfTrickEvent(e);
            case EndOfRoundIOEvent e -> renderEndOfRoundEvent(e);
            case TrickHistoryIOEvent t -> renderTrickHistoryEvent(t);
            case ParticipatingPlayersIOEvent e -> renderParticipatingPlayersEvent(e);
            // case BotCardIOEvent e -> renderBotCardEvent(e); //TODO is this even used?
            // --- bid state ---
            case BidTurnIOEvent e -> renderBidTurnEvent(e);
            case SuitSelectionIOEvent ignored -> renderSuitSelectionEvent();
            case ProposalRejectedIOEvent e -> renderRejectedProposalEvent(e);
            case BiddingCompletedIOEvent ignored -> renderBiddingCompleteEvent();
            // --- count state ---
            case BidSelectionIOEvent e -> renderBidSelectionEvent(e);
            case PlayerSelectionIOEvent e -> renderPlayerSelectionEvent(e);
            case TrickInputIOEvent ignored -> renderTrickInputEvent();
            case ScoreBoardIOEvent e -> renderScoreBoardEvent(e);
            case SaveDescriptionIOEvent ignored -> renderSaveDescriptionEvent();
            // --- menu ---
            case WelcomeMenuIOEvent ignored -> renderWelcomeMenuEvent();
            case AmountOfBotsIOEvent ignored -> renderAmountOfBotsEvent();
            case PlayerNameIOEvent e -> renderPlayerNameEvent(e);
            case BotStrategyIOEvent e -> renderBotStrategyEvent(e);
            case PrintNamesIOEvent e -> renderPrintNamesEvent(e);
            case MessageIOEvent t -> renderMessageEvent(t);
            case LoadSaveIOEvent l -> renderLoadSaveEvent(l);
            default -> throw new IllegalStateException("Unhandled IOEvent: " + event);
        }
    }

    private void renderLoadSaveEvent(LoadSaveIOEvent l) {
        List<String> saveDescriptions = l.availableSaves();
        System.out.println("\n========================================");
        System.out.println("SELECT A SAVE TO LOAD:");
        for (int i = 0; i < saveDescriptions.size(); i++) {
            System.out.printf("[%2d] %s%n", (i + 1), saveDescriptions.get(i));
        }
        System.out.println("Your choice: ");
    }

    private void renderConfigEvent(ConfirmationIOEvent e) {
        System.out.println("\n========================================");
        System.out.println("  NEXT PLAYER: " + e.playerName().toUpperCase());
        System.out.println("  Pass the device, then press ENTER.");
        System.out.println("========================================");
    }

    private void renderBotCardEvent(BotCardIOEvent e) {
        System.out.println("Bot played " + e.card());
        System.out.println("\n[ Press ENTER to view cards on table ]"); // TODO: wat is dit?

    }

    private void renderMessageEvent(MessageIOEvent t) {
        System.out.println(t.text());
    }

    private void renderPlayCardEvent(PlayCardIOEvent event) {
        PlayCardResult data = event.data();

        System.out.println("\n=============================================");
        System.out.println("  TRICK #" + data.trickNumber() + " | TURN: " + data.player().getName().toUpperCase());
        System.out.println("=============================================");

        // 1. Table Display
        System.out.println("\nCARDS ON TABLE:");
        if (data.turns().isEmpty()) {
            System.out.println("  [ Empty ]");
        } else {
            // Displays cards in a horizontal-ish list for better flow
            String table = String.join("\n | ", data.turns().stream()
                    .map(Turn::toString).toList());
            System.out.println(" | " + table);
        }

        // 2. Open Miserie (Exposed Hands)
        if (data.isOpenMiserie()) {
            System.out.println("\nEXPOSED HANDS (OPEN MISERIE)");
            for (int i = 0; i < data.exposedPlayerNames().size(); i++) {
                String name = data.exposedPlayerNames().get(i);
                List<Card> exposedHand = data.formattedExposedHands().get(i);

                System.out.printf("%-12s : ", name); // Aligns names
                System.out.println(exposedHand);
            }
        }

        // 3. Player's Own Hand & Controls
        System.out.println("\n---------------------------------------------");
        System.out.println("YOUR HAND (" + data.player().getName() + "):");

        List<Card> hand = data.legalCards();
        for (int i = 0; i < hand.size(); i++) {
            // Padded index for alignment (e.g., [ 1] vs [10])
            System.out.printf("  [%2d] %s%n", (i + 1), hand.get(i));
        }

        System.out.println("\n[ 0] View Last Trick History");
        System.out.print("SELECT CARD INDEX: ");
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

        System.out.println("Available Options:");

        List<BidType> bids = event.data().availableBids();

        for (int i = 0; i < bids.size(); i++) {
            System.out.println("   [" + (i + 1) + "] " + bids.get(i).name());
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
        System.out.println(
                event.multi() ? "Select all players involved (participating or winners) (comma-separated, 0 for none):"
                        : "Select the main bidder:");
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

    private void renderEndOfTurnEvent(EndOfTurnIOEvent event) {
        System.out.println(event.data().name() + " played " + event.data().card());
    }

    private void renderEndOfTrickEvent(EndOfTrickIOEvent event) {
        System.out.println(event.data().name() + " played " + event.data().card());
        System.out.println(event.data().winner() + " won the trick!");
        System.out.println("\n============== NEXT TRICK ==============");
    }

    private void renderEndOfRoundEvent(EndOfRoundIOEvent event) {
        System.out.println(event.data().name() + " played " + event.data().card());
        System.out.println("\n============== ROUND OVER ==============");
        System.out.println("Calculating final scores...");
    }

    private void renderTrickHistoryEvent(TrickHistoryIOEvent event) {
        System.out.println("-------------- LAST PLAYED TRICK ---------------");
        event.data().trick().getTurns().forEach(t -> System.out.println("- " + t));
        System.out.println("------------------------------------------------");
    }

    private void renderParticipatingPlayersEvent(ParticipatingPlayersIOEvent event) {
        System.out.println("Select the participating players (comma-separated):");
        List<String> names = event.data().playerNames();
        for (int i = 0; i < names.size(); i++) {
            System.out.println("   [" + (i + 1) + "] " + names.get(i));
        }
        System.out.print("Your choice: ");
    }
}