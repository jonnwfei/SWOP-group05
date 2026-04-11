package base.domain.round;

import base.domain.bid.*;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.strategy.LowBotStrategy;
import base.domain.player.Player;
import base.domain.trick.Trick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundTest {

    private Player p1, p2, p3, p4;
    private List<Player> players;
    private Round round;

    @BeforeEach
    void setUp() {
        p1 = new Player(new LowBotStrategy(), "P1");
        p2 = new Player(new LowBotStrategy(), "P2");
        p3 = new Player(new LowBotStrategy(), "P3");
        p4 = new Player(new LowBotStrategy(), "P4");

        players = List.of(p1, p2, p3, p4);
        round = new Round(players, p1, 1);
    }

    // -------- HELPER METHODS --------

    private Trick createRealTrick(Player winner) {
        Trick trick = new Trick(p1, null);

        Card low1 = new Card(Suit.HEARTS, Rank.TWO);
        Card low2 = new Card(Suit.HEARTS, Rank.THREE);
        Card low3 = new Card(Suit.HEARTS, Rank.FOUR);
        Card winningCard = new Card(Suit.HEARTS, Rank.ACE);

        p1.setHand(new ArrayList<>(List.of(winner == p1 ? winningCard : low1)));
        p2.setHand(new ArrayList<>(List.of(winner == p2 ? winningCard : low2)));
        p3.setHand(new ArrayList<>(List.of(winner == p3 ? winningCard : low3)));
        p4.setHand(new ArrayList<>(List.of(winner == p4 ? winningCard : low1)));

        trick.playCard(p1, p1.getHand().getFirst());
        trick.playCard(p2, p2.getHand().getFirst());
        trick.playCard(p3, p3.getHand().getFirst());
        trick.playCard(p4, p4.getHand().getFirst());

        return trick;
    }

    // -------- CONSTRUCTOR and STATE --------
    @Test
    void constructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Round(null, p1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Round(List.of(p1, p2, p3), p1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Round(players, null, 1));

        Player p5 = new Player(new LowBotStrategy(), "P5");
        assertThrows(IllegalArgumentException.class, () -> new Round(players, p5, 1));
    }

    @Test
    void advanceToNextPlayer() {
        assertEquals(p1, round.getCurrentPlayer());
        round.advanceToNextPlayer();
        assertEquals(p2, round.getCurrentPlayer());
        round.advanceToNextPlayer();
        assertEquals(p3, round.getCurrentPlayer());
        round.advanceToNextPlayer();
        assertEquals(p4, round.getCurrentPlayer());
        round.advanceToNextPlayer();
        assertEquals(p1, round.getCurrentPlayer());
    }

    // -------- TRICK REGISTRATION TESTS --------
    @Test
    void registerCompletedTrick_ThrowsOnIncompleteTrick() {
        Trick incompleteTrick = new Trick(p1, Suit.HEARTS);
        p1.setHand(new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)))); // Use ArrayList
        incompleteTrick.playCard(p1, new Card(Suit.HEARTS, Rank.ACE));

        assertThrows(IllegalArgumentException.class, () -> round.registerCompletedTrick(incompleteTrick));
    }

    @Test
    void registerCompletedTrick_UpdatesState() {
        Trick trick = createRealTrick(p2);

        // Prevent NullPointer inside the trigger of calculateScores() by giving it a bid!
        round.setHighestBid(new SoloProposalBid(p1));

        round.registerCompletedTrick(trick);

        assertEquals(1, round.getTricks().size());
        assertEquals(trick, round.getLastPlayedTrick());
        assertEquals(p2, round.getCurrentPlayer());
    }

    // -------- SCORE CALCULATION TESTS --------
    @Test
    void calculateScoresForCount_ThrowsIfHighestBidNull() {
        assertThrows(IllegalStateException.class, () ->
                round.calculateScoresForCount(5, List.of(p1), null));
    }

    @Test
    void calculateScoresForCount_Miserie() {
        Bid miserieBid = new MiserieBid(p1, BidType.OPEN_MISERIE);
        round.setHighestBid(miserieBid);

        assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(0, null, List.of(p1)));
        assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(0, List.of(), List.of(p1)));

        round.calculateScoresForCount(0, List.of(p1, p2), List.of(p1));

        assertEquals(56, p1.getScore());
        assertEquals(-56, p2.getScore());
        assertEquals(0, p3.getScore());
        assertEquals(0, p4.getScore());
    }

    @Test
    void calculateScoresForCount_NormalBid() {
        Bid normalBid = new SoloProposalBid(p1);

        round.setHighestBid(normalBid);
        round.calculateScoresForCount(5, List.of(p1), null);

        assertEquals(6, p1.getScore());
        assertEquals(-2, p2.getScore());
        assertEquals(-2, p3.getScore());
        assertEquals(-2, p4.getScore());
    }

    // -------- SCORE CALCULATION (END OF ROUND) --------
    @Test
    void calculateScoresThrowsRoundNotComplete() {
        assertThrows(IllegalStateException.class, () -> round.calculateScores());
        assertEquals(0, p1.getScore());
    }

    @Test
    void calculateScores_SoloBid_1v3() {
        Bid soloBid = new SoloBid(p1, BidType.SOLO, Suit.HEARTS);
        round.setHighestBid(soloBid);
        round.setBids(List.of(soloBid));

        for (int i = 0; i < 13; i++) round.registerCompletedTrick(createRealTrick(p1));

        // Testing based on your Bid formulas
        assertEquals(75, p1.getScore());
        assertEquals(-25, p2.getScore());
        assertEquals(-25, p3.getScore());
        assertEquals(-25, p4.getScore());
    }

    @Test
    void calculateScores_NormalBid_2v2() {
        Bid proposal = new ProposalBid(p1);
        Bid acceptance = new AcceptedBid(p2);

        round.setBids(List.of(proposal, acceptance));
        round.setHighestBid(acceptance);

        for (int i = 0; i < 5; i++) round.registerCompletedTrick(createRealTrick(p3));
        for (int i = 0; i < 8; i++) round.registerCompletedTrick(createRealTrick(p1));

        assertEquals(2, (int) p1.getScore());
        assertEquals(2, (int) p2.getScore());
        assertEquals(-2, (int) p3.getScore());
        assertEquals(-2, (int) p4.getScore());
    }

    @Test
    void calculateScores_Miserie() {
        Bid miserieBid = new MiserieBid(p1, BidType.MISERIE);

        round.setBids(List.of(miserieBid));
        round.setHighestBid(miserieBid);

        for (int i = 0; i < 13; i++) round.registerCompletedTrick(createRealTrick(p2));

        // Verify the Miserie player won
        assertEquals(21, p1.getScore()); // Assuming base points are 50
        // Verify the defenders were punished
        assertEquals(-7, p2.getScore()); // 50 / 3 = 16.66 (Integer division floors it to 16)
        assertEquals(-7, p3.getScore());
        assertEquals(-7, p4.getScore());
    }

    @Test
    void calculateScores_Miserie2() {
        Bid miserieBid1 = new MiserieBid(p1, BidType.MISERIE);
        Bid miserieBid2 = new MiserieBid(p2, BidType.MISERIE);

        round.setBids(List.of(miserieBid1,miserieBid2));
        round.setHighestBid(miserieBid1);

        for (int i = 0; i < 13; i++) round.registerCompletedTrick(createRealTrick(p3));

        // Verify the Miserie player won
        assertEquals(14, p1.getScore()); // Assuming base points are 50
        // Verify the defenders were punished
        assertEquals(14, p2.getScore()); // 50 / 3 = 16.66 (Integer division floors it to 16)
        assertEquals(-14, p3.getScore());
        assertEquals(-14, p4.getScore());
    }

    @Test
    void calculateScores_Miserie2Loss() {
        Bid miserieBid1 = new MiserieBid(p1, BidType.MISERIE);
        Bid miserieBid2 = new MiserieBid(p2, BidType.MISERIE);

        round.setBids(List.of(miserieBid1,miserieBid2));
        round.setHighestBid(miserieBid1);

        round.registerCompletedTrick(createRealTrick(p1));
        round.registerCompletedTrick(createRealTrick(p2));
        for (int i = 0; i < 11; i++) round.registerCompletedTrick(createRealTrick(p3));

        // Verify the Miserie player won
        assertEquals(-14, p1.getScore()); // Assuming base points are 50
        // Verify the defenders were punished
        assertEquals(-14, p2.getScore()); // 50 / 3 = 16.66 (Integer division floors it to 16)
        assertEquals(14, p3.getScore());
        assertEquals(14, p4.getScore());
    }

    // -------- WINNER DETERMINATION TESTS --------
    @Test
    void getWinningPlayers_NotFinished() {
        round.setHighestBid(new SoloProposalBid(p1)); // Prevent null pointer
        assertTrue(round.getWinningPlayers().isEmpty());
    }

    @Test
    void getWinningPlayers_Miserie() {
        Bid miserieBid = new MiserieBid(p1, BidType.OPEN_MISERIE);
        round.setBids(List.of(miserieBid));
        round.setHighestBid(miserieBid);

        for (int i = 0; i < 13; i++) round.registerCompletedTrick(createRealTrick(p2));

        List<Player> winners = round.getWinningPlayers();
        assertTrue(winners.contains(p1));
        assertEquals(1, winners.size());
    }

    @Test
    void getWinningPlayers_NormalBid_WinAndLose() {
        // Test Win Scenario
        Bid winningBid = new SoloProposalBid(p1);
        round.setHighestBid(winningBid);
        round.setBids(List.of(winningBid));

        for (int i = 0; i < 13; i++) round.registerCompletedTrick(createRealTrick(p1));
        assertTrue(round.getWinningPlayers().contains(p1));

        // Test Lose Scenario (Using a real failing bid, not PassBid)
        Round losingRound = new Round(players, p1, 1);
        Bid losingBid = new SoloProposalBid(p1); // P1 says "I'll take 5"
        losingRound.setHighestBid(losingBid);
        losingRound.setBids(List.of(losingBid));

        // But P2 wins every single trick!
        for (int i = 0; i < 13; i++) losingRound.registerCompletedTrick(createRealTrick(p2));

        List<Player> winners = losingRound.getWinningPlayers();
        assertFalse(winners.contains(p1)); // P1 failed his bid!
        assertTrue(winners.contains(p2));  // Defenders win!
    }

    // -------- GETTERS n SETTERS for Coverage --------
    @Test
    void gettersAndSetters() {
        round.setTrumpSuit(Suit.HEARTS);
        assertEquals(Suit.HEARTS, round.getTrumpSuit());

        assertEquals(players, round.getPlayers());
        assertTrue(round.getBids().isEmpty());

        round.setCurrentPlayer(p3);
        assertEquals(p3, round.getCurrentPlayer());

        Bid bid = new PassBid(p1);
        round.setHighestBid(bid);
        assertEquals(bid, round.getHighestBid());

        assertThrows(IllegalArgumentException.class, () -> round.setBids(null));
        round.setBids(List.of(bid));
        assertEquals(1, round.getBids().size());

        assertNull(round.getLastPlayedTrick());
        assertFalse(round.isFinished());
    }
}