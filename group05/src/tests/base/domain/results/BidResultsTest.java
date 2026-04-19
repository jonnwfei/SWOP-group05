package base.domain.results;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BidResults Data Transfer Objects & Validation")
class BidResultsTest {

    // Complex entity can still be mocked safely
    @Mock private Player mockPlayer;

    private final Card testCard = new Card(Suit.HEARTS, Rank.ACE);
    private final BidType testBidType = BidType.SOLO;

    @Nested
    @DisplayName("Architectural Integrity")
    class ArchitectureTests {
        @Test
        @DisplayName("BidResults must be a sealed interface with strict permitted subclasses")
        void verifySealedInterface() {
            assertTrue(BidResults.class.isInterface(), "BidResults should be an interface.");
            assertTrue(BidResults.class.isSealed(), "BidResults must be sealed.");

            List<Class<?>> permitted = Arrays.asList(BidResults.class.getPermittedSubclasses());
            assertEquals(4, permitted.size(), "Should have exactly 4 permitted subclasses.");
            assertTrue(permitted.contains(BidResults.BidTurnResult.class));
            assertTrue(permitted.contains(BidResults.SuitSelectionRequired.class));
            assertTrue(permitted.contains(BidResults.ProposalRejected.class));
            assertTrue(permitted.contains(BidResults.BiddingCompleted.class));
        }
    }

    @Nested
    @DisplayName("BidTurnResult Record")
    class BidTurnResultTests {

        @Test
        @DisplayName("Successfully creates with valid arguments and returns via accessors")
        void validCreationAndAccessors() {
            List<BidType> bids = List.of(testBidType);
            List<Card> hand = List.of(testCard);

            BidResults.BidTurnResult result = new BidResults.BidTurnResult(
                    "Alice", Suit.HEARTS, testBidType, bids, hand, mockPlayer
            );

            assertEquals("Alice", result.playerName());
            assertEquals(Suit.HEARTS, result.trumpSuit());
            assertEquals(testBidType, result.currentHighestBid());
            assertEquals(bids, result.availableBids());
            assertEquals(hand, result.hand());
            assertEquals(mockPlayer, result.player());
        }

        @Test
        @DisplayName("Defensively copies lists to ensure true immutability")
        void defensiveCopying() {
            List<BidType> mutableBids = new ArrayList<>(List.of(testBidType));
            List<Card> mutableHand = new ArrayList<>(List.of(testCard));

            BidResults.BidTurnResult result = new BidResults.BidTurnResult(
                    "Alice", Suit.HEARTS, testBidType, mutableBids, mutableHand, mockPlayer
            );

            // Mutate the original lists
            mutableBids.clear();
            mutableHand.clear();

            // The record's lists should remain unaffected
            assertFalse(result.availableBids().isEmpty(), "availableBids should be a defensive copy.");
            assertFalse(result.hand().isEmpty(), "hand should be a defensive copy.");

            // Verify the copies are strictly unmodifiable
            assertThrows(UnsupportedOperationException.class, () -> result.availableBids().clear());
            assertThrows(UnsupportedOperationException.class, () -> result.hand().clear());
        }

        @Test
        @DisplayName("Rejects null or invalid arguments thoroughly")
        void validationGuards() {
            List<BidType> validBids = List.of(testBidType);
            List<Card> validHand = List.of(testCard);

            // playerName
            assertThrows(IllegalArgumentException.class, () -> new BidResults.BidTurnResult(null, Suit.HEARTS, testBidType, validBids, validHand, mockPlayer));
            assertThrows(IllegalArgumentException.class, () -> new BidResults.BidTurnResult("   ", Suit.HEARTS, testBidType, validBids, validHand, mockPlayer));

            // availableBids
            assertThrows(IllegalArgumentException.class, () -> new BidResults.BidTurnResult("Alice", Suit.HEARTS, testBidType, null, validHand, mockPlayer));
            assertThrows(IllegalArgumentException.class, () -> new BidResults.BidTurnResult("Alice", Suit.HEARTS, testBidType, Collections.emptyList(), validHand, mockPlayer));

            // hand
            assertThrows(IllegalArgumentException.class, () -> new BidResults.BidTurnResult("Alice", Suit.HEARTS, testBidType, validBids, null, mockPlayer));

            // player
            assertThrows(IllegalArgumentException.class, () -> new BidResults.BidTurnResult("Alice", Suit.HEARTS, testBidType, validBids, validHand, null));
        }

        @Test
        @DisplayName("Validates implicit record methods (equals, hashCode, toString)")
        void implicitMethods() {
            List<BidType> bids = List.of(testBidType);
            List<Card> hand = List.of(testCard);

            BidResults.BidTurnResult r1 = new BidResults.BidTurnResult("Alice", Suit.HEARTS, testBidType, bids, hand, mockPlayer);
            BidResults.BidTurnResult r2 = new BidResults.BidTurnResult("Alice", Suit.HEARTS, testBidType, bids, hand, mockPlayer);
            BidResults.BidTurnResult r3 = new BidResults.BidTurnResult("Bob", Suit.HEARTS, testBidType, bids, hand, mockPlayer);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertNotEquals(r1, r3);
            assertTrue(r1.toString().contains("Alice"));
        }
    }

    @Nested
    @DisplayName("SuitSelectionRequired Record")
    class SuitSelectionRequiredTests {

        @Test
        @DisplayName("Successfully creates with valid arguments and returns via accessors")
        void validCreationAndAccessors() {
            Suit[] suits = {Suit.HEARTS, Suit.SPADES};

            BidResults.SuitSelectionRequired result = new BidResults.SuitSelectionRequired(
                    "Bob", testBidType, suits
            );

            assertEquals("Bob", result.playerName());
            assertEquals(testBidType, result.pendingBid());
            assertArrayEquals(suits, result.availableSuits());
        }

        @Test
        @DisplayName("Defensively clones the array to ensure true immutability")
        void defensiveCopying() {
            Suit[] mutableSuits = {Suit.HEARTS};

            BidResults.SuitSelectionRequired result = new BidResults.SuitSelectionRequired(
                    "Bob", testBidType, mutableSuits
            );

            // Mutate the original array
            mutableSuits[0] = Suit.SPADES;

            // The record's array should remain unaffected
            assertEquals(Suit.HEARTS, result.availableSuits()[0], "Array should be cloned upon construction.");
        }

        @Test
        @DisplayName("Rejects null or invalid arguments thoroughly")
        void validationGuards() {
            Suit[] validSuits = {Suit.HEARTS};

            // playerName
            assertThrows(IllegalArgumentException.class, () -> new BidResults.SuitSelectionRequired(null, testBidType, validSuits));
            assertThrows(IllegalArgumentException.class, () -> new BidResults.SuitSelectionRequired("   ", testBidType, validSuits));

            // pendingBid
            assertThrows(IllegalArgumentException.class, () -> new BidResults.SuitSelectionRequired("Bob", null, validSuits));

            // availableSuits
            assertThrows(IllegalArgumentException.class, () -> new BidResults.SuitSelectionRequired("Bob", testBidType, null));
            assertThrows(IllegalArgumentException.class, () -> new BidResults.SuitSelectionRequired("Bob", testBidType, new Suit[0]));
        }

        @Test
        @DisplayName("Validates implicit record methods recognizing native array reference equality")
        void implicitMethods() {
            Suit[] suits1 = {Suit.HEARTS};
            Suit[] suits2 = {Suit.HEARTS}; // Different array instance in memory, same content

            BidResults.SuitSelectionRequired r1 = new BidResults.SuitSelectionRequired("Bob", testBidType, suits1);
            BidResults.SuitSelectionRequired r1_reference = r1; // Exact same object in memory
            BidResults.SuitSelectionRequired r2 = new BidResults.SuitSelectionRequired("Bob", testBidType, suits2);

            // 1. Coverage for native equals() & hashCode()
            // Native records use reference equality (==) for arrays.
            assertEquals(r1, r1_reference, "Same instances should be equal");
            assertEquals(r1.hashCode(), r1_reference.hashCode());
            assertNotEquals(r1, r2, "Different array instances evaluate to false in native records");

            // 2. The Industry-Standard way to assert equality on records with arrays:
            // Test the scalar fields normally...
            assertEquals(r1.playerName(), r2.playerName());
            assertEquals(r1.pendingBid(), r2.pendingBid());
            // ...and test the array fields using assertArrayEquals!
            assertArrayEquals(r1.availableSuits(), r2.availableSuits(), "Array contents should match");

            // 3. Coverage for native toString()
            assertTrue(r1.toString().contains("Bob"));
            assertTrue(r1.toString().contains("SuitSelectionRequired"));
        }
    }

    @Nested
    @DisplayName("ProposalRejected Record")
    class ProposalRejectedTests {

        @Test
        @DisplayName("Creates successfully and validates arguments")
        void creationAndValidation() {
            BidResults.ProposalRejected r = new BidResults.ProposalRejected("Charlie");
            assertEquals("Charlie", r.playerName());

            assertThrows(IllegalArgumentException.class, () -> new BidResults.ProposalRejected(null));
            assertThrows(IllegalArgumentException.class, () -> new BidResults.ProposalRejected(""));
            assertThrows(IllegalArgumentException.class, () -> new BidResults.ProposalRejected("   "));
        }

        @Test
        @DisplayName("Validates implicit record methods")
        void implicitMethods() {
            BidResults.ProposalRejected r1 = new BidResults.ProposalRejected("Charlie");
            BidResults.ProposalRejected r2 = new BidResults.ProposalRejected("Charlie");

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("Charlie"));
        }
    }

    @Nested
    @DisplayName("BiddingCompleted Record")
    class BiddingCompletedTests {

        @Test
        @DisplayName("Creates successfully and implements implicit methods")
        void creationAndImplicitMethods() {
            BidResults.BiddingCompleted r1 = new BidResults.BiddingCompleted();
            BidResults.BiddingCompleted r2 = new BidResults.BiddingCompleted();

            assertNotNull(r1);
            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("BiddingCompleted"));
        }
    }
}