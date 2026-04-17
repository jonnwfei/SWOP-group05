package base.domain.strategy;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.PlayerId;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Smart Bot Memory components.
 * Smarter bots must remember which cards have been played to judge master cards[cite: 244, 246, 247].
 */
@DisplayName("Smart Bot Memory (Observer & Logic)")
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
    @DisplayName("Observer Event Handling")
    class EventHandlingTests {

        @Test
        @DisplayName("onRoundStarted should reset all memory buffers")
        void shouldResetMemoryOnNewRound() {
            // Arrange
            memory.onTrumpDetermined(Suit.HEARTS);
            memory.onBidPlaced(new BidTurn(p1, BidType.SOLO));

            // Act
            memory.onRoundStarted(List.of(p1, p2, p3, p4));

            // Assert
            assertNull(memory.getCurrentTrump(), "Trump suit should be cleared on new round.");
            assertNull(memory.getHighestBid(), "Bid history should be cleared on new round.");
            assertTrue(memory.isLeadPlayer(), "Lead status should reset to true.");
        }

        @Test
        @DisplayName("onTurnPlayed should track unplayed cards (Card Counting)")
        void shouldTrackUnplayedCards() {
            Card aceOfSpades = new Card(Suit.SPADES, Rank.ACE);
            memory.onTurnPlayed(new PlayTurn(p1, aceOfSpades));

            // Per project rules, bots remember played cards to judge if a card is master[cite: 247].
            assertTrue(memory.isHighestUnplayedCardInSuit(aceOfSpades), "Ace should be tracked in the played pool.");

            Card kingOfSpades = new Card(Suit.SPADES, Rank.KING);
            assertTrue(memory.isHighestUnplayedCardInSuit(kingOfSpades), "King becomes the master unplayed card.");
        }
    }

    @Nested
    @DisplayName("Trick Evaluation Logic")
    class TrickLogicTests {

        @BeforeEach
        void initTrick() {
            memory.onTrumpDetermined(Suit.CLUBS); // Clubs are trump [cite: 158]
        }

        @Test
        @DisplayName("getCurrentWinningTurn identifies high card of lead suit")
        void shouldIdentifyWinningLeadSuit() {
            // Lead suit is Hearts [cite: 162]
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.TEN)));
            memory.onTurnPlayed(new PlayTurn(p2, new Card(Suit.HEARTS, Rank.QUEEN)));

            PlayTurn winner = memory.getCurrentWinningTurn();
            assertEquals(p2, winner.playerId(), "Queen of hearts should beat Ten of hearts.");
        }

        @Test
        @DisplayName("getCurrentWinningTurn identifies trump winner")
        void shouldIdentifyTrumpWinner() {
            // Trump cards beat all cards from other suits [cite: 160, 161]
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.ACE)));
            memory.onTurnPlayed(new PlayTurn(p2, new Card(Suit.CLUBS, Rank.TWO)));

            assertEquals(p2, memory.calculateCurrentWinnerId(), "Trump 2 should beat Ace of non-trump suit.");
        }
    }

    @Nested
    @DisplayName("Partnership & Team Logic")
    class TeamLogicTests {

        @Test
        @DisplayName("isTeamWinning correctly identifies partner in Proposal/Acceptance")
        void shouldIdentifyPartnershipWinning() {
            // Arrange: Start the round properly so the memory knows the 4 players at the table
            memory.onRoundStarted(List.of(p1, p2, p3, p4));

            // Arrange: P1 Proposes, P2 Passes, P3 Accepts, P4 Passes.
            // We MUST have 4 bids to signify the end of the bidding phase!
            memory.onBidPlaced(new BidTurn(p1, BidType.PROPOSAL));
            memory.onBidPlaced(new BidTurn(p2, BidType.PASS));
            memory.onBidPlaced(new BidTurn(p3, BidType.ACCEPTANCE));
            memory.onBidPlaced(new BidTurn(p4, BidType.PASS));

            memory.onTrumpDetermined(Suit.HEARTS);

            // Act: P1 (Partner) plays a winning card
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.ACE)));

            // Assert: Partnership recognition is vital for bot heuristics
            assertTrue(memory.isTeamWinning(p3), "P3 should recognize that their partner is winning the trick.");
            assertFalse(memory.isTeamWinning(p2), "Opponents should recognize their team is not winning.");
        }

        @Test
        @DisplayName("isTeamWinning throws exception if no bids recorded")
        void shouldThrowIfNoBids() {
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.ACE)));

            assertThrows(IllegalStateException.class, () -> memory.isTeamWinning(p1),
                    "Should throw if attempting to check teams before bidding phase concludes.");
        }
    }

    @Nested
    @DisplayName("Card Counting Logic")
    class CardCountingTests {

        @Test
        @DisplayName("isHighestUnplayedCardInSuit detects if better cards are gone")
        void shouldDetectMasterCard() {
            Card kingOfHearts = new Card(Suit.HEARTS, Rank.KING);
            Card aceOfHearts = new Card(Suit.HEARTS, Rank.ACE);

            // Initially, King is not the master [cite: 159]
            assertFalse(memory.isHighestUnplayedCardInSuit(kingOfHearts), "King is not the highest if Ace is unplayed.");

            // Ace is played [cite: 246]
            memory.onTurnPlayed(new PlayTurn(p4, aceOfHearts));

            // Now King is the master
            assertTrue(memory.isHighestUnplayedCardInSuit(kingOfHearts), "King should be identified as the highest remaining unplayed card.");
        }
    }
}