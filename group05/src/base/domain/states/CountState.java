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

            // Phase 1: Validatie Bid Type
            if (phase == 1) {
                int bidChoice = Integer.parseInt(input);
                if (bidChoice < 1 || bidChoice > 10) {
                    return new QuestionEvent("Invalid choice. Please pick a number between 1 and 10:");
                }
                this.numberBid = bidChoice;
                phase = 2;
                return new QuestionEvent("What Suit is the trump suit?\n" +
                        "(1) Hearts (2) Clubs (3) Diamonds (4) Spades");
            }

            // Phase 2: Validatie Troef
            if (phase == 2) {
                this.numberTrump = Integer.parseInt(input);
                this.trumpSuit = switch (numberTrump) {
                    case 1 -> Suit.HEARTS;
                    case 2 -> Suit.CLUBS;
                    case 3 -> Suit.DIAMONDS;
                    case 4 -> Suit.SPADES;
                    default -> null;
                };

                if (this.trumpSuit == null) {
                    return new QuestionEvent("Invalid suit. Please pick (1) Hearts, (2) Clubs, (3) Diamonds or (4) Spades:");
                }
                phase = 3;
                return new QuestionEvent("Which player numbers played this bid? (e.g., '0' or '0, 2')\n" + getGame().printNames());
            }

            // Phase 3: Validatie Spelers
            if (phase == 3) {
                this.participatingPlayers = Arrays.stream(input.split("[^0-9]+"))
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .toList();

                if (participatingPlayers.isEmpty()) {
                    return new QuestionEvent("Please enter at least one player number:");
                }

                // Check of player indices bestaan
                for (int idx : participatingPlayers) {
                    if (idx < 0 || idx >= getGame().getPlayers().size()) {
                        return new QuestionEvent("Invalid player number: " + idx + ". Try again:");
                    }
                }

                phase = 4;
                if (numberBid == 7 || numberBid == 8) {
                    return new QuestionEvent("Which players won their bid? (Numbers of players who got 0 tricks): \n" + getGame().printNames());
                } else {
                    return new QuestionEvent("How many tricks did the player(s) win?");
                }
            }

            // Phase 4: Berekening en Resultaat
            if (phase == 4) {
                Player firstPlayer = getGame().getPlayers().get(participatingPlayers.get(0));

                // Mapping van keuze naar Bid object
                this.bid = switch (numberBid) {
                    case 1 -> new SoloProposalBid(firstPlayer); // Alone op dealt trump
                    case 2 -> new ProposalBid(firstPlayer);   // Wordt later AcceptedBid
                    case 3 -> new AbondanceBid(firstPlayer, ABONDANCE_9, trumpSuit);
                    case 4 -> new AbondanceBid(firstPlayer, ABONDANCE_10, trumpSuit);
                    case 5 -> new AbondanceBid(firstPlayer, ABONDANCE_11, trumpSuit);
                    case 6 -> new AbondanceBid(firstPlayer, ABONDANCE_12_OT, trumpSuit);
                    case 7 -> new MiserieBid(firstPlayer, MISERIE);
                    case 8 -> new MiserieBid(firstPlayer, OPEN_MISERIE);
                    case 9 -> new SoloBid(firstPlayer, SOLO, trumpSuit);
                    case 10 -> new SoloBid(firstPlayer, SOLO_SLIM, trumpSuit);
                    default -> throw new IllegalStateException("Unexpected value: " + numberBid);
                };

                Round round = new Round(getGame().getPlayers(), null);
                getGame().addRound(round);
                round.setHighestBid(bid);

                if (numberBid == 7 || numberBid == 8) {
                    // TODO: Implementeer specifieke Miserie-winnaars logica
                    // Bijvoorbeeld: parse input voor spelers die 0 slagen haalden
                } else {
                    int aantalSlagen = Integer.parseInt(input);
                    if (aantalSlagen < 0 || aantalSlagen > 13) {
                        return new QuestionEvent("A round has between 0 and 13 tricks. Please enter a valid number:");
                    }
                    round.calculateScores(aantalSlagen, null);
                }

                phase = 5;
                return new QuestionEvent(getGame().printScore() + "\n" +
                        "Do you want to: \n(1) Simulate another round\n(2) Go back to the main menu");
            }

            if (phase == 5) {
                keuze = Integer.parseInt(input);
                if (keuze != 1 && keuze != 2) {
                    return new QuestionEvent("Please choose (1) or (2):");
                }
                return new TextEvent("Transitioning...");
            }

        } catch (NumberFormatException e) {
            return new QuestionEvent("Invalid input. Please enter a number:");
        }

        return new TextEvent("Finalizing state...");
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