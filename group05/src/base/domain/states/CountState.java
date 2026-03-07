package base.domain.states;

import base.domain.WhistGame;
import base.domain.player.Player;
import base.domain.round.Round;
import cli.elements.GameEvent;
import cli.elements.QuestionEvent;
import cli.elements.TextEvent;
import base.domain.bid.*;
import base.domain.card.Suit;

import java.util.Arrays;
import java.util.List;
import static base.domain.bid.BidType.*;

public class CountState extends State {
    private int phase = 0;
    private int numberBid;
    private int numberTrump;
    private int keuze;
    private Bid bid;
    private Suit trumpSuit;
    private List<Integer> participatingPlayers;

    public CountState(WhistGame game) {
        super(game);
        this.numberBid = 0;
        this.numberTrump = 0;
        this.participatingPlayers = null;
    }

    @Override
    public GameEvent executeState(String input) {
        try {
            // Phase 0: Welkomstbericht tonen
            if (phase == 0) {
                phase = 1;
                return new QuestionEvent("===== WELCOME TO THE COUNT ==== \n" + "WHICH ROUND WAS PLAYED? \n" +
                        "Proposal: \n(1) Alone    (2) With Partner\n" +
                        "Abondance:\n(3) 9   (4) 10   (5) 11   (6) 12\n" +
                        "Miserie:\n(7) Normal       (8) Open\n" +
                        "Solo:\n(9) Normal       (10) Solo Slim\n");
            }

            if (phase == 1) {
                int bidChoice = Integer.parseInt(input);
                if (bidChoice < 1 || bidChoice > 10) return new QuestionEvent("Invalid choice. Please pick 1-10:");
                this.numberBid = bidChoice;
                phase = 2;
                return new QuestionEvent("What Suit is the trump suit?\n(1) Hearts (2) Clubs (3) Diamonds (4) Spades");
            }

            if (phase == 2) {
                this.numberTrump = Integer.parseInt(input);
                this.trumpSuit = switch (numberTrump) {
                    case 1 -> Suit.HEARTS; case 2 -> Suit.CLUBS;
                    case 3 -> Suit.DIAMONDS; case 4 -> Suit.SPADES;
                    default -> null;
                };
                if (this.trumpSuit == null) return new QuestionEvent("Invalid suit. Pick (1-4):");
                phase = 3;
                return new QuestionEvent("Which player numbers played this bid? (e.g., '0' or '0, 2')\n" + getGame().printNames());
            }

            if (phase == 3) {
                this.participatingPlayers = Arrays.stream(input.split("[^0-9]+"))
                        .filter(s -> !s.isEmpty()).map(Integer::parseInt).toList();

                if (participatingPlayers.isEmpty()) return new QuestionEvent("Enter at least one player number:");
                for (int idx : participatingPlayers) {
                    if (idx < 0 || idx >= getGame().getPlayers().size()) return new QuestionEvent("Invalid player: " + idx);
                }

                phase = 4;
                if (numberBid == 7 || numberBid == 8) {
                    return new QuestionEvent("Which players won their bid? (Numbers of players who got 0 tricks): \n" + getGame().printNames());
                } else {
                    return new QuestionEvent("How many tricks did the player(s) win?");
                }
            }

            if (phase == 4) {
                // 1. Haal de volledige lijst van deelnemende Player objecten op
                List<Player> participants = participatingPlayers.stream()
                        .map(idx -> getGame().getPlayers().get(idx)).toList();

                Player firstPlayer = participants.get(0);

                // 2. Initialiseer de Bid
                this.bid = switch (numberBid) {
                    case 1 -> new SoloProposalBid(firstPlayer);
                    case 2 -> new ProposalBid(firstPlayer); // Wordt intern in Round afgehandeld als partnerschap
                    case 3 -> new AbondanceBid(firstPlayer, ABONDANCE_9, trumpSuit);
                    case 4 -> new AbondanceBid(firstPlayer, ABONDANCE_10, trumpSuit);
                    case 5 -> new AbondanceBid(firstPlayer, ABONDANCE_11, trumpSuit);
                    case 6 -> new AbondanceBid(firstPlayer, ABONDANCE_12_OT, trumpSuit);
                    case 7 -> new MiserieBid(firstPlayer, MISERIE);
                    case 8 -> new MiserieBid(firstPlayer, OPEN_MISERIE);
                    case 9 -> new SoloBid(firstPlayer, SOLO, trumpSuit);
                    case 10 -> new SoloBid(firstPlayer, SOLO_SLIM, trumpSuit);
                    default -> throw new IllegalStateException("Unexpected bid index");
                };

                // 3. Maak de ronde aan en zet de hoogste bid
                Round round = new Round(getGame().getPlayers(), null);
                round.setHighestBid(bid);
                getGame().addRound(round);

                // 4. Score berekening op basis van type
                if (numberBid == 7 || numberBid == 8) {
                    // Input zijn de player indices die 0 slagen haalden (winnende miserie spelers)
                    List<Integer> winnersIndices = Arrays.stream(input.split("[^0-9]+"))
                            .filter(s -> !s.isEmpty()).map(Integer::parseInt).toList();

                    List<Player> winningMiseriePlayers = winnersIndices.stream()
                            .map(idx -> getGame().getPlayers().get(idx)).toList();

                    // Bij miserie is tricksWon irrelevant (we sturen 0)
                    round.calculateScoresForCount(0, participants, winningMiseriePlayers);
                } else {
                    int aantalSlagen = Integer.parseInt(input);
                    if (aantalSlagen < 0 || aantalSlagen > 13) return new QuestionEvent("Enter a number between 0-13:");

                    // Bij normale biedingen is winningMiseriePlayers null
                    round.calculateScoresForCount(aantalSlagen, participants, null);
                }

                phase = 5;
                return new QuestionEvent(getGame().printScores() + "\n" +
                        "Do you want to: \n(1) Simulate another round\n(2) Go back to the main menu");
            }

            if (phase == 5) {
                keuze = Integer.parseInt(input);
                if (keuze != 1 && keuze != 2) return new QuestionEvent("Please choose (1) or (2):");
                return new TextEvent("Processing choice...");
            }

        } catch (NumberFormatException e) {
            System.err.println("Input parsing error: " + input);
            return new QuestionEvent("Invalid input. Please enter a valid number:");
        } catch (Exception e) {
            System.err.println("CRITICAL: " + e.getMessage());
            throw new IllegalStateException("Flow failed in Phase " + phase, e);
        }

        return new TextEvent("Finalizing...");
    }

    @Override

    public State nextState() {
        if (keuze == 1){

            return new CountState(getGame());

        }
        else {

            return new MenuState(getGame());

        }
    }
}