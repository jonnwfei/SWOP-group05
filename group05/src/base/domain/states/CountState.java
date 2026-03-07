package base.domain.states;

import base.domain.WhistGame;

import base.domain.player.Player;
import base.domain.round.Round;
import cli.elements.GameEvent;
import cli.elements.QuestionEvent;

import java.util.Arrays;
import java.util.List;

import base.domain.bid.*;
import base.domain.card.Suit;
import cli.elements.TextEvent;

import static base.domain.bid.BidType.*;

/**
* @author Stan Kestens
 *@since 06/03
 * In this State,
*/
public class CountState extends State {
    private int phase = 0; // 0: Bid, 1: Trump, 2: Players, 3: Result
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
        // Phase 0: Ask for the Bid type
        if (phase == 0) {
            phase = 1;
            return new QuestionEvent("===== WELCOME TO THE COUNT==== \n" + "WHICH ROUND WAS PLAYED? \n" +
                    "Proposal: \n"+
                    "(1) Alone    (2) With Partner\n"+
                    "Abondance (amount of cards)\n"+
                    "(3) 9   (4) 10   (5) 11   (6) 12\n"+
                    "Miserie\n"+
                    "(7) Normal       (8) Open          \n"+
                    "Solo\n"+
                    "(9) Normal       (10) Slim         \n");
        }
        // Phase 1: Save Bid, Ask for Trump
        else if (phase == 1) {
            this.numberBid = Integer.parseInt(input);

            phase = 2;
            return new QuestionEvent("What Suit is the trump suit?\n" +
                    "(1) Hearts (2) Clubs (3) Diamonds (4) Spades");
        }
        // Phase 2: Save Trump, Ask for Players
        else if (phase == 2) {
            this.numberTrump = Integer.parseInt(input);
            this.trumpSuit = switch (numberTrump) {
                case 1 -> Suit.HEARTS;
                case 2 -> Suit.CLUBS;
                case 3 -> Suit.DIAMONDS;
                case 4 -> Suit.SPADES;
                default -> null; // You should add error handling here
            };
            phase = 3;
            return new QuestionEvent("Which player numbers played this bid?" + getGame().printNames());

        }

        // Phase 3: Save Players, Ask for Result (Guideline 6)
        else if (phase == 3) {
            List<Integer> participatingPlayers = Arrays.stream(input.split("[^0-9]+"))
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
            phase = 4;

            // Guideline 6(a): Miserie (Options 7 and 8)
            if (numberBid == 7 || numberBid == 8) {
                return new QuestionEvent("Which players won their bid? :" + getGame().printNames());
            }
            // Guideline 6(b): All other bids
            else {
                return new QuestionEvent("How many tricks did the player(s) of the registered bid win?");
            }
        }
        else if (phase == 4){
            //hier is input dus in geval van miserie : de winnaars van hun bid, anders hoeveel tricks gewonnen
            //
            Round round = new Round(getGame().getPlayers(),null);
            Player firstPlayer = getGame().getPlayers().get(participatingPlayers.getFirst());
            switch (numberBid) {
                case 1 -> this.bid = new ProposalBid(firstPlayer); // Alone
                case 2 -> this.bid = new ProposalBid(firstPlayer);   // With Partner
                case 3 -> this.bid = new AbondanceBid(firstPlayer, ABONDANCE_9, trumpSuit);
                case 4 -> this.bid = new AbondanceBid(firstPlayer, ABONDANCE_10, trumpSuit);
                case 5 -> this.bid = new AbondanceBid(firstPlayer, ABONDANCE_11, trumpSuit);
                case 6 -> this.bid = new AbondanceBid(firstPlayer, ABONDANCE_12_OT, trumpSuit);
                case 7 -> this.bid = new MiserieBid(firstPlayer, MISERIE); // Normal
                case 8 -> this.bid = new MiserieBid(firstPlayer, OPEN_MISERIE);  // Open
                case 9 -> this.bid = new SoloBid(firstPlayer, SOLO, trumpSuit);    // Normal
                case 10 -> this.bid = new SoloBid(firstPlayer, SOLO_SLIM, trumpSuit);    // Slim
                default -> {
                    return new QuestionEvent("Invalid choice. Please pick 1-10:");
                }
            }
            getGame().addRound(round);
            round.setHighestBid(bid);
            if (numberBid == 7 || numberBid == 8) {
                //TODO
            }
            // Guideline 6(b): All other bids
            else {
                int aantalSlagen = Integer.parseInt(input);
                round.calculateScores(aantalSlagen, null);//aantal bids gewonne : -1 bij miserie + lijst spelers da gewonne heeft bij miserie
            }

            String nextStatePrep = getGame().printScores() +
                    "Do you want to : \n (1) Simulate another round\n (2) Go back to the main menu"  ;


        }
        else{
            keuze = Integer.parseInt(input);
            return new TextEvent("");
        }

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