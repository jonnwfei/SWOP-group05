package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.LowBotStrategy;
import base.domain.player.Player;
import base.domain.trick.Trick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundTest {

    private Player p1, p2, p3, p4;
    private List<Player> players;
    private Round round;

    @BeforeEach
    void setUp() {
        // Instantiate 4 REAL players using your bot strategy so they don't require UI input
        p1 = new Player(new LowBotStrategy(), "P1");
        p2 = new Player(new LowBotStrategy(), "P2");
        p3 = new Player(new LowBotStrategy(), "P3");
        p4 = new Player(new LowBotStrategy(), "P4");

        players = List.of(p1, p2, p3, p4);

        // Initialize a round with a 1x multiplier
        round = new Round(players, p1, 1);
    }

    // -------- HELPER METHODS --------

    /**
     * Creates a real, fully completed Trick by putting cards in players' hands
     * and having them legally play them. It ensures the 'winner' parameter wins.
     */
    private Trick createRealTrick(Player winner) {
        Trick trick = new Trick(p1, null); // No trump suit for simplicity

        // 4 cards of the same suit. The Ace will naturally win.
        Card low1 = new Card(Suit.HEARTS, Rank.TWO);
        Card low2 = new Card(Suit.HEARTS, Rank.THREE);
        Card low3 = new Card(Suit.HEARTS, Rank.FOUR);
        Card winningCard = new Card(Suit.HEARTS, Rank.ACE);
        
        // Distribute the winning card to the target winner, and low cards to everyone else
        p1.setHand(List.of(winner == p1 ? winningCard : low1));
        p2.setHand(List.of(winner == p2 ? winningCard : low2));
        p3.setHand(List.of(winner == p3 ? winningCard : low3));
        p4.setHand(List.of(winner == p4 ? winningCard : low1)); // low1 reused cuz sws one has winningCard

        trick.playCard(p1, p1.getHand().getFirst());
        trick.playCard(p2, p2.getHand().getFirst());
        trick.playCard(p3, p3.getHand().getFirst());
        trick.playCard(p4, p4.getHand().getFirst());

        return trick;
    }

    /**
     * A pure Java stub for the Bid class. This allows us to control the points
     * without needing complex actual Bid logic.
     */
    static class TestingBid implements Bid {
        private final BidType type;
        private final Player player;
        private final int fixedPoints;

        public TestingBid(Player player, BidType type, int fixedPoints) {
            this.player = player;
            this.type = type;
            this.fixedPoints = fixedPoints;
        }

        @Override public BidType getType() { return type; }
        @Override public Player getPlayer() { return player; }
        @Override public int calculateBasePoints(int tricksWon) { return fixedPoints; }

        @Override
        public Suit getChosenTrump(Suit dealtTrump) {
            return null;
        }
    }


    // -------- CONSTRUCTOR and STATE --------
    @Test
    void constructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Round(null, p1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Round(List.of(p1, p2, p3), p1, 1)); // Only 3 players
        assertThrows(IllegalArgumentException.class, () -> new Round(players, null, 1));

        Player p5 = new Player(new LowBotStrategy(), "P5");
        assertThrows(IllegalArgumentException.class, () -> new Round(players, p5, 1)); // Dealer not in list
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
        assertEquals(p1, round.getCurrentPlayer()); // Cycles back
    }

    // -------- TRICK REGISTRATION TESTS --------
    @Test
    void registerCompletedTrick_ThrowsOnIncompleteTrick() {
        Trick incompleteTrick = new Trick(p1, Suit.HEARTS);
        p1.setHand(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        incompleteTrick.playCard(p1, new Card(Suit.HEARTS, Rank.ACE)); // Only 1 turn played

        assertThrows(IllegalArgumentException.class, () -> round.registerCompletedTrick(incompleteTrick));
    }

    @Test
    void registerCompletedTrick_UpdatesState() {
        Trick trick = createRealTrick(p2);
        round.registerCompletedTrick(trick);

        assertEquals(1, round.getTricks().size());
        assertEquals(trick, round.getLastPlayedTrick());
        assertEquals(p2, round.getCurrentPlayer()); // Winner becomes next player
    }

    // -------- SCORE CALCULATION TESTS --------
    @Test
    void calculateScoresForCount_ThrowsIfHighestBidNull() {
        assertThrows(IllegalStateException.class, () ->
                round.calculateScoresForCount(5, List.of(p1), null));
    }

    @Test
    void calculateScoresForCount_Miserie() {
        Bid miserieBid = new TestingBid(p1, BidType.OPEN_MISERIE, 30);
        round.setHighestBid(miserieBid);

        // Null participants throws error
        assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(0, null, null));

        // p1 wins miserie (0 tricks), p2 loses (1+ tricks)
        round.calculateScoresForCount(0, List.of(p1, p2), List.of(p1));

        // Using real player getScore() to verify math
        assertEquals(30, p1.getScore());
        assertEquals(-30, p2.getScore()); // Basepoints calculated using fixed negative logic in TestingBid
    }

    @Test
    void calculateScoresForCount_NormalBid() {
        Bid normalBid = new TestingBid(p1, BidType.PROPOSAL, 30);

        round.setHighestBid(normalBid);
        round.calculateScoresForCount(5, List.of(p1), null);

        assertEquals(30, p1.getScore());
        // Opponents pay 1/3 of the base points
        assertEquals(-10, p2.getScore());
        assertEquals(-10, p3.getScore());
        assertEquals(-10, p4.getScore());
    }

    // -------- SCORE CALCULATION (END OF ROUND) --------
    @Test
    void calculateScores_EarlyReturnIfNotFinished() {
        round.calculateScores(); // Should do nothing since trick size is 0
        assertEquals(0, p1.getScore()); // Score remains untouched
    }

    @Test
    void calculateScores_NormalBid_1v3() {
        Bid soloBid = new TestingBid(p1, BidType.PROPOSAL, 30);

        round.setHighestBid(soloBid);
        round.setBids(List.of(soloBid));

        // P1 wins all 13 tricks to finish the round
        for (int i = 0; i < 13; i++) round.registerCompletedTrick(createRealTrick(p1));

        assertEquals(30, p1.getScore());
        assertEquals(-10, p2.getScore());
    }

    @Test
    void calculateScores_NormalBid_2v2() {
        Bid proposal = new TestingBid(p1, BidType.PROPOSAL, 20);
        Bid acceptance = new TestingBid(p2, BidType.ACCEPTANCE, 20);

        round.setBids(List.of(proposal, acceptance));
        round.setHighestBid(acceptance);

        // Attackers win all tricks
        for (int i = 0; i < 13; i++) round.registerCompletedTrick(createRealTrick(p1));

        assertEquals(20, p1.getScore());
        assertEquals(20, p2.getScore());
        assertEquals(-20, p3.getScore()); // Opponents pay full in 2v2
        assertEquals(-20, p4.getScore());
    }

    @Test
    void calculateScores_Miserie() {
        Bid miserieBid = new TestingBid(p1, BidType.OPEN_MISERIE, 50);

        round.setBids(List.of(miserieBid));
        round.setHighestBid(miserieBid);

        // P2 wins all tricks, meaning P1 successfully took 0 tricks!
        for (int i = 0; i < 13; i++) round.registerCompletedTrick(createRealTrick(p2));

        assertEquals(50, p1.getScore());
    }


    // -------- WINNER DETERMINATION TESTS --------
    @Test
    void getWinningPlayers_NotFinished() {
        assertTrue(round.getWinningPlayers().isEmpty());
    }

    @Test
    void getWinningPlayers_Miserie() {
        Bid miserieBid = new TestingBid(p1, BidType.OPEN_MISERIE, 50);

        round.setBids(List.of(miserieBid));
        round.setHighestBid(miserieBid);

        for (int i = 0; i < 13; i++) round.registerCompletedTrick(createRealTrick(p2)); // p1 takes 0 tricks

        List<Player> winners = round.getWinningPlayers();
        assertTrue(winners.contains(p1));
        assertEquals(1, winners.size());
    }

    @Test
    void getWinningPlayers_NormalBid_WinAndLose() {
        // Test Win Scenario (Positive Points)
        Bid winningBid = new TestingBid(p1, BidType.PROPOSAL, 50);
        round.setHighestBid(winningBid);
        round.setBids(List.of(winningBid));

        for (int i = 0; i < 13; i++) round.registerCompletedTrick(createRealTrick(p1));
        assertTrue(round.getWinningPlayers().contains(p1));

        // Test Lose Scenario (Negative Points)
        Round losingRound = new Round(players, p1, 1);
        Bid losingBid = new TestingBid(p1, BidType.PROPOSAL, -50);
        losingRound.setHighestBid(losingBid);
        losingRound.setBids(List.of(losingBid));

        for (int i = 0; i < 13; i++) losingRound.registerCompletedTrick(createRealTrick(p2));

        List<Player> winners = losingRound.getWinningPlayers();
        assertFalse(winners.contains(p1)); // P1 lost
        assertTrue(winners.contains(p2));  // Defenders won
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

        Bid bid = new TestingBid(p1, BidType.PASS, 0);
        round.setHighestBid(bid);
        assertEquals(bid, round.getHighestBid());

        assertThrows(IllegalArgumentException.class, () -> round.setBids(null));
        round.setBids(List.of(bid));
        assertEquals(1, round.getBids().size());

        assertNull(round.getLastPlayedTrick());
        assertFalse(round.isFinished());
    }
}