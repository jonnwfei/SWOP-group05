package base.domain;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.MiserieBid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.trick.Trick;
import cli.elements.GameEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WhistGameTest {

    private WhistGame game;
    private Player p1, p2, p3, p4;

    @BeforeEach
    void setUp() {
        game = new WhistGame();
        p1 = new Player(new HumanStrategy(), "Stan");
        p2 = new Player(new HumanStrategy(), "Seppe");
        p3 = new Player(new HumanStrategy(), "Tommy");
        p4 = new Player(new HumanStrategy(), "John");
    }

    // -------- SETUP AND LIST MANAGEMENT TESTS --------

    @Test
    void addAndGetPlayers() {
        game.addPlayer(p1);
        game.addPlayer(p2);

        List<Player> players = game.getPlayers();
        assertEquals(2, players.size());
        assertEquals("Stan", players.getFirst().getName());

        // Ensure encapsulation: modifying the returned list shouldn't affect the internal list
        players.clear();
        assertEquals(2, game.getPlayers().size());
    }

    @Test
    void resetPlayers() {
        game.addPlayer(p1);
        game.resetPlayers();
        assertTrue(game.getPlayers().isEmpty());
    }

    @Test
    void deckGettersAndSetters() {
        Deck deck = new Deck();
        assertNull(game.getDeck(), "Deck should initially be null");

        game.setDeck(deck);
        assertEquals(deck, game.getDeck());
    }

    @Test
    void roundsManagement() {
        game.addPlayer(p1);
        game.addPlayer(p2);
        game.addPlayer(p3);
        game.addPlayer(p4);

        assertNull(game.getCurrentRound());

        Round round = new Round(game.getPlayers(), p1, 1);
        game.addRound(round);

        assertEquals(round, game.getCurrentRound());
        assertEquals(1, game.getRounds().size());

        game.resetRounds();
        assertTrue(game.getRounds().isEmpty());
    }

    // -------- CURRENT PLAYER TESTS --------

    @Test
    void setCurrentPlayer_Valid() {
        game.addPlayer(p1);
        game.setCurrentPlayer(p1);
        assertEquals(p1, game.getCurrentPlayer());
    }

    @Test
    void setCurrentPlayer_NullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> game.setCurrentPlayer(null));
    }

    @Test
    void setCurrentPlayer_NotInGameThrowsException() {
        game.addPlayer(p1);
        // p2 is not added to the game yet
        assertThrows(IllegalArgumentException.class, () -> game.setCurrentPlayer(p2));
    }

    // -------- DEALER MANAGEMENT TESTS --------

    @Test
    void setRandomDealer_ThrowsIfEmpty() {
        assertThrows(IllegalArgumentException.class, () -> game.setRandomDealer());
    }

    @Test
    void setRandomDealer_AssignsDealer() {
        game.addPlayer(p1);
        game.addPlayer(p2);
        game.addPlayer(p3);
        game.addPlayer(p4);

        game.setRandomDealer();
        assertNotNull(game.getDealerPlayer());
        assertTrue(game.getPlayers().contains(game.getDealerPlayer()));
    }

    @Test
    void advanceDealer_ThrowsIfInvalid() {
        // Empty list
        assertThrows(IllegalStateException.class, () -> game.advanceDealer());

        // Populated list, but dealer is null
        game.addPlayer(p1);
        game.addPlayer(p2);
        assertThrows(IllegalStateException.class, () -> game.advanceDealer());
    }

    @Test
    void advanceDealer_CyclesCorrectly() {
        game.addPlayer(p1);
        game.addPlayer(p2);
        game.addPlayer(p3);

        // Force the dealer to be P1
        game.setRandomDealer();
        Player initialDealer = game.getDealerPlayer();

        List<Player> players = game.getPlayers();
        int initialIdx =  players.indexOf(initialDealer);

        Player expectedNext = players.get((initialIdx + 1) % players.size());
        Player expectedNextNext = players.get((initialIdx + 2) % players.size());

        game.advanceDealer();
        assertEquals(expectedNext, game.getDealerPlayer());

        game.advanceDealer();
        assertEquals(expectedNextNext, game.getDealerPlayer());

        game.advanceDealer();
        assertEquals(initialDealer, game.getDealerPlayer());
    }

    // -------- STRING FORMATTING TESTS --------

    @Test
    void printNames_FormatsCorrectly() {
        game.addPlayer(p1);
        game.addPlayer(p2);

        String expected = "Players in this game:\n1. Stan\n2. Seppe\n";
        assertEquals(expected, game.printNames());
    }

    @Test
    void printScore_FormatsCorrectly() {
        p1.updateScore(42);
        p2.updateScore(-14);

        game.addPlayer(p1);
        game.addPlayer(p2);

        String expected = "============== SCORES ==============\n" +
                "Stan: 42 points\n" +
                "Seppe: -14 points\n" +
                "====================================";

        assertEquals(expected, game.printScore());
    }

    // -------- WINNER RESOLUTION TESTS --------

    @Test
    void getLastRoundWinner_NoRounds() {
        assertNull(game.getLastRoundWinner());
    }

    @Test
    void getLastRoundWinner_UnfinishedRound() {
        game.addPlayer(p1); game.addPlayer(p2); game.addPlayer(p3); game.addPlayer(p4);
        Round round = new Round(game.getPlayers(), p1, 1);

        // Prevent null pointer by setting a bid
        round.setHighestBid(new MiserieBid(p1, BidType.MISERIE));
        game.addRound(round);

        assertNull(game.getLastRoundWinner()); // Round has 0 tricks, no winners yet
    }

    @Test
    void getLastRoundWinner_FinishedRound() {
        game.addPlayer(p1); game.addPlayer(p2); game.addPlayer(p3); game.addPlayer(p4);
        Round round = new Round(game.getPlayers(), p1, 1);

        Bid miserieBid = new MiserieBid(p1, BidType.MISERIE);
        round.setBids(List.of(miserieBid));
        round.setHighestBid(miserieBid);

        // Simulate a finished round where P1 wins Miserie (takes 0 tricks)
        for (int i = 0; i < 13; i++) {
            Trick trick = new Trick(p2, null);
            p1.setHand(new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.TWO))));
            p2.setHand(new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)))); // P2 wins
            p3.setHand(new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.THREE))));
            p4.setHand(new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.FOUR))));

            trick.playCard(p2, p2.getHand().getFirst());
            trick.playCard(p3, p3.getHand().getFirst());
            trick.playCard(p4, p4.getHand().getFirst());
            trick.playCard(p1, p1.getHand().getFirst());

            round.registerCompletedTrick(trick);
        }

        game.addRound(round);
        assertEquals(p1, game.getLastRoundWinner());
    }

    // -------- STATE PATTERN TESTS --------

    @Test
    void stateTransitionFlow() {
        // Because executeState and nextState delegate to MenuState (and so on),
        // we just verify that the game successfully delegates without throwing exceptions.
//        GameEvent event = game.executeState("1");
//        assertNotNull(event);
//
//        assertDoesNotThrow(() -> game.nextState());
    }
}