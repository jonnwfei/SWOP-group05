package tests.base.domain.strategy;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.PlayerId;
import base.domain.strategy.SmartBotMemory;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the internal memory states, event processing, and defensive guardrails
 * of the SmartBotMemory observer.
 */
@DisplayName("Smart Bot Memory (Observer & State Container)")
class SmartBotMemoryTest {

    private SmartBotMemory memory;
    private final PlayerId p1 = new PlayerId();
    private final PlayerId p2 = new PlayerId();
    private final PlayerId p3 = new PlayerId();
    private final PlayerId p4 = new PlayerId();

    @BeforeEach
    void setUp() {
        memory = new SmartBotMemory();
    }

    @Nested
    @DisplayName("Observer Event Handling & State Resets")
    class EventHandlingTests {

        @Test
        @DisplayName("onRoundStarted completely clears and resets all state buffers")
        void shouldResetMemoryOnNewRound() {
            // Arrange: Dirty the state
            memory.onRoundStarted(List.of(p1, p2, p3, p4));
            memory.onTrumpDetermined(Suit.HEARTS);
            memory.onBidPlaced(new BidTurn(p1, BidType.SOLO));
            memory.onBiddingFinalized(BidType.SOLO, List.of(p1));
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.ACE)));

            // Act: Trigger a new round
            memory.onRoundStarted(List.of(p1, p2, p3, p4));

            // Assert: Everything should be wiped clean
            assertNull(memory.getCurrentTrump(), "Trump suit should be cleared");
            assertNull(memory.getHighestBid(), "Bid history should be cleared");
            assertNull(memory.getActiveBid(), "Active contract should be cleared");
            assertFalse(memory.isPlayerOnBiddingTeam(p1), "Bidding team should be cleared");
            assertNull(memory.getLeadSuit(), "Lead suit should be null for empty trick");
            assertTrue(memory.isLeadPlayer(), "Should be the lead player again");
            assertFalse(memory.hasPlayerActedInCurrentTrick(p1), "Trick plays should be cleared");
        }

        @Test
        @DisplayName("onTrickCompleted immediately resets the table for the next trick")
        void shouldClearTrickOnTrickCompleted() {
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.TWO)));
            memory.onTurnPlayed(new PlayTurn(p2, new Card(Suit.HEARTS, Rank.THREE)));

            assertFalse(memory.isLeadPlayer(), "Trick is currently active");
            assertNotNull(memory.getLeadSuit(), "Lead suit exists");

            // Act
            memory.onTrickCompleted(p2);

            // Assert
            assertTrue(memory.isLeadPlayer(), "Trick buffer should be wiped clean");
            assertNull(memory.getLeadSuit(), "Lead suit should be reset");
            assertNull(memory.getCurrentWinnerId(), "Winner ID should be reset");
        }

        @Test
        @DisplayName("Engine events aggressively throw exceptions on null or corrupt data")
        void engineDefensiveChecks() {
            assertThrows(IllegalArgumentException.class, () -> memory.onRoundStarted(null));
            assertThrows(IllegalArgumentException.class, () -> memory.onRoundStarted(List.of()));
            assertThrows(IllegalArgumentException.class, () -> memory.onBidPlaced(null));
            assertThrows(IllegalArgumentException.class, () -> memory.onBiddingFinalized(BidType.SOLO, null));
            assertThrows(IllegalArgumentException.class, () -> memory.onTurnPlayed(null));
            assertThrows(IllegalArgumentException.class, () -> memory.onTurnPlayed(new PlayTurn(null, new Card(Suit.HEARTS, Rank.ACE))));
        }
    }

    @Nested
    @DisplayName("Bidding Queries")
    class BiddingQueryTests {

        @Test
        @DisplayName("getHighestBid returns null if no bids exist")
        void getHighestBid_Empty() {
            assertNull(memory.getHighestBid());
        }

        @Test
        @DisplayName("getHighestBid evaluates correctly based on BidType comparisons")
        void getHighestBid_EvaluatesProperly() {
            memory.onBidPlaced(new BidTurn(p1, BidType.PASS));
            memory.onBidPlaced(new BidTurn(p2, BidType.PROPOSAL));
            memory.onBidPlaced(new BidTurn(p3, BidType.SOLO)); // Highest
            memory.onBidPlaced(new BidTurn(p4, BidType.ACCEPTANCE));

            BidTurn highest = memory.getHighestBid();
            assertNotNull(highest);
            assertEquals(BidType.SOLO, highest.bidType());
            assertEquals(p3, highest.playerId());
        }

        @Test
        @DisplayName("hasActiveProposal evaluates true only if a PROPOSAL exists")
        void hasActiveProposal() {
            assertFalse(memory.hasActiveProposal());

            memory.onBidPlaced(new BidTurn(p1, BidType.PASS));
            assertFalse(memory.hasActiveProposal());

            memory.onBidPlaced(new BidTurn(p2, BidType.PROPOSAL));
            assertTrue(memory.hasActiveProposal());
        }

        @Test
        @DisplayName("onBiddingFinalized stores the active bid and tracks the bidding team")
        void biddingFinalizationTracking() {
            memory.onBiddingFinalized(BidType.SOLO, List.of(p2));

            assertEquals(BidType.SOLO, memory.getActiveBid());
            assertTrue(memory.isPlayerOnBiddingTeam(p2));
            assertFalse(memory.isPlayerOnBiddingTeam(p1));
            assertFalse(memory.isPlayerOnBiddingTeam(p3));
            assertFalse(memory.isPlayerOnBiddingTeam(p4));
        }
    }

    @Nested
    @DisplayName("Trick & Player Queries")
    class TrickQueryTests {

        @Test
        @DisplayName("Trick queries safely return null when the trick is empty")
        void emptyTrickQueries() {
            assertNull(memory.getLeadSuit());
            assertNull(memory.getCurrentWinningCard());
            assertNull(memory.getCurrentWinnerId());
        }

        @Test
        @DisplayName("Player trick queries gracefully handle null inputs without crashing")
        void gracefulNullHandling() {
            assertFalse(memory.hasPlayerActedInCurrentTrick(null));
            assertNull(memory.getCardPlayedBy(null));
        }

        @Test
        @DisplayName("Accurately identifies player actions and cards in the current trick")
        void playerActionTracking() {
            Card playedCard = new Card(Suit.SPADES, Rank.TEN);
            memory.onTurnPlayed(new PlayTurn(p1, playedCard));

            assertTrue(memory.hasPlayerActedInCurrentTrick(p1));
            assertFalse(memory.hasPlayerActedInCurrentTrick(p2));

            assertEquals(playedCard, memory.getCardPlayedBy(p1));
            assertNull(memory.getCardPlayedBy(p2));
        }

        @Test
        @DisplayName("getCurrentWinningCard and getCurrentWinnerId delegate correctly to the Trick object")
        void trickWinnerTracking() {
            memory.onTrumpDetermined(Suit.CLUBS);

            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.TEN))); // Lead
            memory.onTurnPlayed(new PlayTurn(p2, new Card(Suit.HEARTS, Rank.QUEEN))); // Higher Lead
            memory.onTurnPlayed(new PlayTurn(p3, new Card(Suit.DIAMONDS, Rank.ACE))); // Off-suit (loses)

            assertEquals(p2, memory.getCurrentWinnerId());
            assertEquals(Rank.QUEEN, memory.getCurrentWinningCard().rank());

            memory.onTurnPlayed(new PlayTurn(p4, new Card(Suit.CLUBS, Rank.TWO))); // Trump wins
            assertEquals(p4, memory.getCurrentWinnerId());
            assertEquals(Suit.CLUBS, memory.getCurrentWinningCard().suit());
        }
    }

    @Nested
    @DisplayName("Card Counting Logic")
    class CardCountingTests {

        @Test
        @DisplayName("isHighestUnplayedCardInSuit correctly evaluates master cards dynamically")
        void shouldDetectMasterCard() {
            memory.onRoundStarted(List.of(p1, p2, p3, p4)); // Populates the unplayed deck

            Card kingOfHearts = new Card(Suit.HEARTS, Rank.KING);
            Card aceOfHearts = new Card(Suit.HEARTS, Rank.ACE);

            // Initially, King is NOT the master because the Ace is still in the unplayed deck
            assertFalse(memory.isHighestUnplayedCardInSuit(kingOfHearts));
            assertTrue(memory.isHighestUnplayedCardInSuit(aceOfHearts));

            // The Ace is played
            memory.onTurnPlayed(new PlayTurn(p4, aceOfHearts));

            // Now the King is mathematically the highest unplayed card in that suit
            assertTrue(memory.isHighestUnplayedCardInSuit(kingOfHearts));
        }

        @Test
        @DisplayName("isHighestUnplayedCardInSuit throws strictly on nulls")
        void cardCountingDefensiveChecks() {
            assertThrows(NullPointerException.class, () -> memory.isHighestUnplayedCardInSuit(null));
        }
    }
}