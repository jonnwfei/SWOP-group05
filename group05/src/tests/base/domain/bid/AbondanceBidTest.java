package base.domain.bid;

import base.domain.card.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Abondance Bid Rules & Calculations")
class AbondanceBidTest {

    private BidType abondanceBidType;
    private Suit chosenTrump;
    private AbondanceBid bid;

    @BeforeEach
    void setUp() {
        abondanceBidType = BidType.ABONDANCE_9; // Assumes this belongs to BidCategory.ABONDANCE
        chosenTrump = Suit.SPADES;

        bid = new AbondanceBid(abondanceBidType, chosenTrump);
    }

    @Nested
    @DisplayName("Constructor & Validation Constraints")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor enforces non-null BidType")
        void constructor_NullParameters_ThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                            new AbondanceBid(null, chosenTrump),
                    "Should reject null BidType"
            );
        }

        @Test
        @DisplayName("Constructor rejects BidTypes outside the ABONDANCE category")
        void constructor_InvalidBidCategory_ThrowsIllegalArgumentException() {
            // MISERIE belongs to the MISERIE category, not ABONDANCE
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    new AbondanceBid( BidType.MISERIE, chosenTrump)
            );
            assertTrue(exception.getMessage().contains("ABONDANCE rank"));
        }
    }

    @Nested
    @DisplayName("Interface Implementations & Getters")
    class InterfaceMethodTests {

        @Test
        @DisplayName("Interface methods getPlayerId() and getType() return correct values")
        void interfaceMethodsReturnCorrectly() {
            assertEquals(abondanceBidType, bid.getType(), "getType() must return the underlying bidType");
        }

        @Test
        @DisplayName("Native record accessors return correct values")
        void testRecordAccessors() {
            assertEquals(abondanceBidType, bid.bidType());
            assertEquals(chosenTrump, bid.trump());
        }

        @Test
        @DisplayName("determineTrump() overrides dealt trump with the player's chosen suit")
        void determineTrump_ValidDealtTrump_ReturnsChosenTrump() {
            assertEquals(chosenTrump, bid.determineTrump(Suit.HEARTS));
        }

        @Test
        @DisplayName("determineTrump() throws exception if dealt trump is null")
        void determineTrump_NullDealtTrump_ThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> bid.determineTrump(null));
        }
    }

    @Nested
    @DisplayName("Auto-Generated Record Methods")
    class RecordImplicitMethodsTests {

        @Test
        @DisplayName("Validates auto-generated equality (equals)")
        void testRecordEquality() {
            AbondanceBid sameBid = new AbondanceBid(abondanceBidType, chosenTrump);
            AbondanceBid differentBid = new AbondanceBid(BidType.ABONDANCE_10, Suit.HEARTS);

            assertEquals(bid, sameBid, "Records with identical data should be equal");
            assertNotEquals(bid, differentBid, "Records with different data should not be equal");
            assertNotEquals(bid, null, "Record should not equal null");
            assertNotEquals(bid, new Object(), "Record should not equal an object of a different type");
        }

        @Test
        @DisplayName("Validates auto-generated hashCode")
        void testRecordHashCode() {
            AbondanceBid sameBid = new AbondanceBid(abondanceBidType, chosenTrump);
            assertEquals(bid.hashCode(), sameBid.hashCode(), "Equal records should have the same hash code");
        }

        @Test
        @DisplayName("Validates auto-generated toString")
        void testRecordToString() {
            String toStringResult = bid.toString();

            assertTrue(toStringResult.contains("AbondanceBid"), "Should contain the class name");
            assertTrue(toStringResult.contains(abondanceBidType.toString()), "Should contain the BidType");
            assertTrue(toStringResult.contains(chosenTrump.toString()), "Should contain the Trump suit");
        }
    }
}