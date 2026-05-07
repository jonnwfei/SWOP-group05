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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Validates the internal memory states, event processing, and situational evaluations
 * of the SmartBotMemory observer.
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
    @DisplayName("Observer Event Handling & State Resets")
    class EventHandlingTests {

        @Test
        @DisplayName("onRoundStarted completely clears and resets all state buffers")
        void shouldResetMemoryOnNewRound() {
            // Arrange: Dirty the state
            memory.onTrumpDetermined(Suit.HEARTS);
            memory.onBidPlaced(new BidTurn(p1, BidType.SOLO));
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.ACE)));

            // Act: Trigger a new round
            memory.onRoundStarted(List.of(p1, p2, p3, p4));

            // Assert: Everything should be wiped clean
            assertNull(memory.getCurrentTrump(), "Trump suit should be cleared");
            assertNull(memory.getHighestBid(), "Bid history should be cleared");
            assertNull(memory.getLeadSuit(), "Lead suit should be null for empty trick");
            assertTrue(memory.isLeadPlayer(), "Should be the lead player again");
            assertFalse(memory.hasPlayerActedInCurrentTrick(p1), "Trick plays should be cleared");
        }

        @Test
        @DisplayName("onTurnPlayed lazily rolls over the trick buffer on the first play of a new trick")
        void shouldLazyRolloverTrickBuffer() {
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.TWO)));
            memory.onTurnPlayed(new PlayTurn(p2, new Card(Suit.HEARTS, Rank.THREE)));
            memory.onTurnPlayed(new PlayTurn(p3, new Card(Suit.HEARTS, Rank.FOUR)));

            assertFalse(memory.isLeadPlayer(), "Trick is currently active");

            memory.onTurnPlayed(new PlayTurn(p4, new Card(Suit.HEARTS, Rank.FIVE)));

            assertFalse(memory.isLeadPlayer(), "Trick buffer should NOT clear immediately after 4 plays");
            assertNotNull(memory.calculateCurrentWinnerId(), "Buffer must hold the 4 cards to calculate the winner");
            assertEquals(Suit.HEARTS, memory.getLeadSuit(), "Lead suit should still be HEARTS");

            // 1st play of Trick 2 triggers the rollover wipe
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.SPADES, Rank.ACE)));

            // ASSERT WIPE & RESTART: The buffer should now only contain the Spades Ace
            assertEquals(Suit.SPADES, memory.getLeadSuit(), "Lead suit should be reset to the new trick's lead (SPADES)");
            assertEquals(p1, memory.calculateCurrentWinnerId(), "P1 should currently be winning the new trick");
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
    }

    @Nested
    @DisplayName("Trick & Player Queries")
    class TrickQueryTests {

        @Test
        @DisplayName("Trick queries return null when the trick is empty")
        void emptyTrickQueries() {
            assertNull(memory.getLeadSuit());
            assertNull(memory.getCurrentWinningTurn());
            assertNull(memory.calculateCurrentWinnerId());
        }

        @Test
        @DisplayName("Player queries enforce defensive null checks")
        void defensiveNullChecks() {
            assertThrows(IllegalArgumentException.class, () -> memory.hasPlayerActedInCurrentTrick(null));
            assertThrows(IllegalArgumentException.class, () -> memory.getCardPlayedBy(null));
            assertThrows(IllegalArgumentException.class, () -> memory.isHighestUnplayedCardInSuit(null));
            assertThrows(IllegalArgumentException.class, () -> memory.isTeamWinning(null));
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
        @DisplayName("getCurrentWinningTurn and calculateCurrentWinnerId track the trick leader")
        void trickWinnerTracking() {
            memory.onTrumpDetermined(Suit.CLUBS);

            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.TEN))); // Lead
            memory.onTurnPlayed(new PlayTurn(p2, new Card(Suit.HEARTS, Rank.QUEEN))); // Higher Lead
            memory.onTurnPlayed(new PlayTurn(p3, new Card(Suit.DIAMONDS, Rank.ACE))); // Off-suit (loses)

            assertEquals(p2, memory.calculateCurrentWinnerId());
            assertEquals(Rank.QUEEN, memory.getCurrentWinningTurn().playedCard().rank());

            memory.onTurnPlayed(new PlayTurn(p4, new Card(Suit.CLUBS, Rank.TWO))); // Trump wins
            assertEquals(p4, memory.calculateCurrentWinnerId());
        }
    }

    @Nested
    @DisplayName("Card Counting Logic")
    class CardCountingTests {

        @Test
        @DisplayName("isHighestUnplayedCardInSuit correctly evaluates master cards dynamically")
        void shouldDetectMasterCard() {
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
    }

    @Nested
    @DisplayName("Partnership & Team Logic")
    class TeamLogicTests {

        @Test
        @DisplayName("isTeamWinning throws IllegalStateException if evaluated before bidding concludes")
        void throwsIfEvaluatedTooEarly() {
            assertThrows(IllegalStateException.class, () -> memory.isTeamWinning(p1));

            memory.onRoundStarted(List.of(p1, p2, p3, p4));
            memory.onBidPlaced(new BidTurn(p1, BidType.PASS));
            // Only 1 bid placed, needs 4. Should throw.
            assertThrows(IllegalStateException.class, () -> memory.isTeamWinning(p1));
        }

        @Test
        @DisplayName("isTeamWinning returns false if no one is currently winning the trick")
        void returnsFalseIfNoWinnerYet() {
            completeBiddingPhase(BidType.PROPOSAL, BidType.PASS, BidType.ACCEPTANCE, BidType.PASS);
            assertFalse(memory.isTeamWinning(p1));
        }

        @Test
        @DisplayName("isTeamWinning returns true if the asking player is themselves winning")
        void returnsTrueIfSelfIsWinning() {
            completeBiddingPhase(BidType.PASS, BidType.PASS, BidType.PASS, BidType.SOLO);
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.SPADES, Rank.ACE)));

            assertTrue(memory.isTeamWinning(p1));
        }

        @Test
        @DisplayName("isTeamWinning evaluates Proposal/Acceptance partnerships correctly")
        void proposalAcceptancePartnerships() {
            // P1 Proposes, P3 Accepts. They are a team. P2 and P4 are the opposing team.
            completeBiddingPhase(BidType.PROPOSAL, BidType.PASS, BidType.ACCEPTANCE, BidType.PASS);

            // P1 is winning the trick
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.SPADES, Rank.ACE)));

            assertTrue(memory.isTeamWinning(p3), "P3 should recognize P1 as their partner.");
            assertFalse(memory.isTeamWinning(p2), "P2 should recognize they are not on the winning team.");
        }

        @Test
        @DisplayName("isTeamWinning returns false for non-partnership bids if partner is winning")
        void soloBidsReturnFalseForOthers() {
            completeBiddingPhase(BidType.PASS, BidType.PASS, BidType.PASS, BidType.SOLO);

            // P4 is winning
            memory.onTurnPlayed(new PlayTurn(p4, new Card(Suit.SPADES, Rank.ACE)));

            // P1 is NOT on P4's team because SOLO is not a partnership bid
            assertFalse(memory.isTeamWinning(p1));
        }

        @Test
        @DisplayName("isTeamWinning defensive guard checks for missing highest bid")
        void defensiveHighestBidGuard() {
            completeBiddingPhase(BidType.PASS, BidType.PASS, BidType.PASS, BidType.PASS);
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.SPADES, Rank.ACE)));

            // Using Mockito Spy to artificially trigger a defensive block in the memory logic
            SmartBotMemory spyMemory = spy(memory);
            doReturn(null).when(spyMemory).getHighestBid();

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> spyMemory.isTeamWinning(p2));
            assertEquals("Highest Bid hasn't been set yet", ex.getMessage());
        }

        /** Helper to quickly populate a valid bidding phase */
        private void completeBiddingPhase(BidType b1, BidType b2, BidType b3, BidType b4) {
            memory.onRoundStarted(List.of(p1, p2, p3, p4));
            memory.onBidPlaced(new BidTurn(p1, b1));
            memory.onBidPlaced(new BidTurn(p2, b2));
            memory.onBidPlaced(new BidTurn(p3, b3));
            memory.onBidPlaced(new BidTurn(p4, b4));
            memory.onTrumpDetermined(Suit.HEARTS);
        }
    }
}