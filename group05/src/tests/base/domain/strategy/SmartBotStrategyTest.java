package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.observer.GameObserver;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Smart Bot Strategy Tests")
class SmartBotStrategyTest {

    private SmartBotStrategy strategy;
    private GameObserver gameObserver;
    private PlayerId myId;
    private PlayerId enemyId;
    private Player mockPlayer;

    @BeforeEach
    void setUp() {
        myId = new PlayerId("Bot-1");
        enemyId = new PlayerId("Enemy-1");
        strategy = new SmartBotStrategy(myId);
        gameObserver = strategy.getGameObserver();

        // Initialize the memory with a fake game start
        gameObserver.onRoundStarted(List.of(myId, enemyId, new PlayerId("P3"), new PlayerId("P4")));

        // Mock the player context passed into the determineBid method
        mockPlayer = mock(Player.class);
        when(mockPlayer.getId()).thenReturn(myId);
    }

    @Nested
    @DisplayName("Defensive Programming & Setup Checks")
    class DefensiveTests {

        @Test
        @DisplayName("Constructor throws on null PlayerId")
        void testNullPlayerId() {
            assertThrows(IllegalArgumentException.class, () -> new SmartBotStrategy(null));
        }

        @Test
        @DisplayName("Requires Confirmation is always false for Bots")
        void testRequiresConfirmation() {
            assertFalse(strategy.requiresConfirmation());
        }

        @Test
        @DisplayName("Throws exception on null player or hand")
        void testNullArguments() {
            assertThrows(IllegalArgumentException.class, () -> strategy.determineBid(null));
            assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(null, Suit.HEARTS));
            assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(new ArrayList<>(), Suit.HEARTS));
        }
    }

    @Nested
    @DisplayName("Bidding Heuristics")
    class BiddingTests {

        @Test
        @DisplayName("Bids OPEN_MISERIE if highest card is <= 7")
        void testOpenMiserieEligibility() {
            List<Card> hand = createHand(Rank.TWO, Rank.FOUR, Rank.SEVEN);
            when(mockPlayer.getHand()).thenReturn(hand);

            Bid bid = strategy.determineBid(mockPlayer);
            assertEquals(BidType.OPEN_MISERIE, bid.getType());
        }

        @Test
        @DisplayName("Bids MISERIE if highest card is <= 10")
        void testMiserieEligibility() {
            List<Card> hand = createHand(Rank.TWO, Rank.EIGHT, Rank.TEN);
            when(mockPlayer.getHand()).thenReturn(hand);

            Bid bid = strategy.determineBid(mockPlayer);
            assertEquals(BidType.MISERIE, bid.getType());
        }

        @Test
        @DisplayName("Bids SOLO_SLIM if expected tricks = 13 and dealt trump is optimal")
        void testSoloSlimBid() {
            gameObserver.onTrumpDetermined(Suit.HEARTS);
            // 13 Hearts = 13 Tricks in Hearts
            List<Card> hand = createSuitHand(Suit.HEARTS, 13);
            when(mockPlayer.getHand()).thenReturn(hand);

            Bid bid = strategy.determineBid(mockPlayer);
            assertEquals(BidType.SOLO_SLIM, bid.getType());
        }

        @Test
        @DisplayName("Bids SOLO if expected tricks = 13 but optimal trump is different")
        void testSoloBid() {
            gameObserver.onTrumpDetermined(Suit.CLUBS); // Dealt clubs
            // 13 Spades = 13 Tricks, but requires changing trump to Spades
            List<Card> hand = createSuitHand(Suit.SPADES, 13);
            when(mockPlayer.getHand()).thenReturn(hand);

            Bid bid = strategy.determineBid(mockPlayer);
            assertEquals(BidType.SOLO, bid.getType());
        }

        @Test
        @DisplayName("Bids PROPOSAL if expected tricks >= 5 and no active proposal")
        void testProposalBid() {
            // 5 Face cards = 5 expected tricks
            List<Card> hand = createHand(Rank.ACE, Rank.KING, Rank.QUEEN, Rank.JACK, Rank.ACE, Rank.TWO);
            when(mockPlayer.getHand()).thenReturn(hand);

            Bid bid = strategy.determineBid(mockPlayer);
            assertEquals(BidType.PROPOSAL, bid.getType());
        }

        @Test
        @DisplayName("Bids ACCEPTANCE if expected tricks >= 3 and active proposal exists")
        void testAcceptanceBid() {
            // 3 Face cards = 3 expected tricks
            List<Card> hand = createHand(Rank.ACE, Rank.KING, Rank.QUEEN, Rank.TWO, Rank.THREE);
            when(mockPlayer.getHand()).thenReturn(hand);

            // Simulate enemy making a proposal
            gameObserver.onBidPlaced(new BidTurn(enemyId, BidType.PROPOSAL));

            Bid bid = strategy.determineBid(mockPlayer);
            assertEquals(BidType.ACCEPTANCE, bid.getType());
        }

        @Test
        @DisplayName("Passes if expected tricks < 5 and no active proposal")
        void testPassBid() {
            // Only 3 expected tricks, but NO proposal to accept
            List<Card> hand = createHand(Rank.ACE, Rank.KING, Rank.QUEEN, Rank.TWO, Rank.THREE);
            when(mockPlayer.getHand()).thenReturn(hand);

            Bid bid = strategy.determineBid(mockPlayer);
            assertEquals(BidType.PASS, bid.getType());
        }
    }

    @Nested
    @DisplayName("Play Phase: Normal Tactic")
    class NormalPlayTests {

        @Test
        @DisplayName("Lead Player: Plays guaranteed winner if holding highest unplayed card")
        void testLeadPlayerGuaranteedWinner() {
            Card highestSpade = new Card(Suit.SPADES, Rank.ACE);
            Card lowSpade = new Card(Suit.SPADES, Rank.TWO);
            List<Card> hand = List.of(highestSpade, lowSpade);

            // Because round just started, Ace of Spades is unplayed. It IS a guaranteed winner.
            Card played = strategy.chooseCardToPlay(hand, null);
            assertEquals(highestSpade, played);
        }

        @Test
        @DisplayName("Not Lead: Plays lowest legal card if team is already winning")
        void testPlayLowestIfWinning() {
            Card highSpade = new Card(Suit.SPADES, Rank.KING);
            Card lowSpade = new Card(Suit.SPADES, Rank.TWO);
            List<Card> hand = List.of(highSpade, lowSpade);

            // Simulate our bot already winning the trick
            gameObserver.onTurnPlayed(new PlayTurn(myId, new Card(Suit.SPADES, Rank.ACE)));

            Card played = strategy.chooseCardToPlay(hand, Suit.SPADES);
            assertEquals(lowSpade, played, "Should conserve high card and play lowest");
        }

        @Test
        @DisplayName("Not Lead, Losing: Plays highest card to follow suit")
        void testPlayHighestToWin() {
            Card highSpade = new Card(Suit.SPADES, Rank.KING);
            Card lowSpade = new Card(Suit.SPADES, Rank.TWO);
            List<Card> hand = List.of(highSpade, lowSpade);

            // Enemy is winning with a Queen
            gameObserver.onTurnPlayed(new PlayTurn(enemyId, new Card(Suit.SPADES, Rank.QUEEN)));

            Card played = strategy.chooseCardToPlay(hand, Suit.SPADES);
            assertEquals(highSpade, played, "Should try to beat the Queen with the King");
        }

        @Test
        @DisplayName("Not Lead, Losing, Void in Lead: Plays lowest trump")
        void testPlayLowestTrumpWhenVoid() {
            gameObserver.onTrumpDetermined(Suit.HEARTS);
            Card highHeart = new Card(Suit.HEARTS, Rank.ACE);
            Card lowHeart = new Card(Suit.HEARTS, Rank.TWO);
            Card club = new Card(Suit.CLUBS, Rank.FIVE);
            List<Card> hand = List.of(highHeart, lowHeart, club); // Void in Spades

            gameObserver.onTurnPlayed(new PlayTurn(enemyId, new Card(Suit.SPADES, Rank.ACE)));

            Card played = strategy.chooseCardToPlay(hand, Suit.SPADES);
            assertEquals(lowHeart, played, "Should trump in with the lowest trump possible");
        }
    }

    @Nested
    @DisplayName("Play Phase: Miserie & Anti-Miserie Tactics (Section 3.3.4)")
    class MiseriePlayTests {

        @BeforeEach
        void setUpMiserie() {
            // Nullify trump for miserie games
            gameObserver.onTrumpDetermined(null);
        }

        @Test
        @DisplayName("MISERIE Tactic: Plays highest safe card that does not win")
        void testMiseriePlaysHighestSafe() {
            // Tell memory our bot is playing Miserie
            gameObserver.onBidPlaced(new BidTurn(myId, BidType.MISERIE));

            Card enemyCard = new Card(Suit.SPADES, Rank.NINE);
            gameObserver.onTurnPlayed(new PlayTurn(enemyId, enemyCard));

            Card lowSafe = new Card(Suit.SPADES, Rank.TWO);
            Card highSafe = new Card(Suit.SPADES, Rank.EIGHT);
            Card unsafeWinning = new Card(Suit.SPADES, Rank.TEN);
            List<Card> hand = List.of(lowSafe, highSafe, unsafeWinning);

            Card played = strategy.chooseCardToPlay(hand, Suit.SPADES);
            assertEquals(highSafe, played, "Should dump the highest possible card that stays under the Nine");
        }

        @Test
        @DisplayName("ANTI-MISERIE: Plays lowest card to keep Miserie player winning")
        void testAntiMiserieForceWin() {
            // Tell memory enemy is playing Miserie
            gameObserver.onBidPlaced(new BidTurn(enemyId, BidType.MISERIE));

            // Enemy plays an 8
            Card enemyCard = new Card(Suit.SPADES, Rank.EIGHT);
            gameObserver.onTurnPlayed(new PlayTurn(enemyId, enemyCard));

            Card lowSafe = new Card(Suit.SPADES, Rank.TWO); // Will lose to 8
            Card highSafe = new Card(Suit.SPADES, Rank.SEVEN); // Will lose to 8
            Card unsafeWinning = new Card(Suit.SPADES, Rank.TEN); // Will beat 8
            List<Card> hand = List.of(lowSafe, highSafe, unsafeWinning);

            Card played = strategy.chooseCardToPlay(hand, Suit.SPADES);
            assertEquals(lowSafe, played, "Should play the absolute lowest to ensure enemy wins the trick");
        }

        @Test
        @DisplayName("ANTI-MISERIE: Plays highest if Miserie player is currently safe")
        void testAntiMiserieMiserieIsSafe() {
            gameObserver.onBidPlaced(new BidTurn(enemyId, BidType.MISERIE));

            // Player 3 is winning with a King. Enemy is safe with a 2.
            gameObserver.onTurnPlayed(new PlayTurn(new PlayerId("P3"), new Card(Suit.SPADES, Rank.KING)));
            gameObserver.onTurnPlayed(new PlayTurn(enemyId, new Card(Suit.SPADES, Rank.TWO)));

            List<Card> hand = List.of(new Card(Suit.SPADES, Rank.FOUR), new Card(Suit.SPADES, Rank.JACK));

            Card played = strategy.chooseCardToPlay(hand, Suit.SPADES);
            assertEquals(Rank.JACK, played.rank(), "Miserie player is already losing the trick, play highest to take control");
        }
    }

    // --- Utility Methods for fast Hand Creation ---

    private List<Card> createHand(Rank... ranks) {
        List<Card> hand = new ArrayList<>();
        // Mix suits arbitrarily for rank-based tests
        Suit[] suits = Suit.values();
        for (int i = 0; i < ranks.length; i++) {
            hand.add(new Card(suits[i % suits.length], ranks[i]));
        }
        return hand;
    }

    private List<Card> createSuitHand(Suit suit, int count) {
        List<Card> hand = new ArrayList<>();
        Rank[] allRanks = Rank.values();
        for (int i = 0; i < count && i < allRanks.length; i++) {
            hand.add(new Card(suit, allRanks[i]));
        }
        return hand;
    }
}