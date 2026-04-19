package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Abondance Bid Rules & Calculations")
class AbondanceBidTest {

    private PlayerId testPlayerId;
    private BidType abondanceBidType;
    private Suit chosenTrump;
    private AbondanceBid bid;

    @BeforeEach
    void setUp() {
        testPlayerId = new PlayerId();
        abondanceBidType = BidType.ABONDANCE_9; // Assumes this belongs to BidCategory.ABONDANCE
        chosenTrump = Suit.SPADES;

        bid = new AbondanceBid(testPlayerId, abondanceBidType, chosenTrump);
    }

    @Nested
    @DisplayName("Constructor & Validation Constraints")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor enforces non-null parameters")
        void constructor_NullParameters_ThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                            new AbondanceBid(null, abondanceBidType, chosenTrump),
                    "Should reject null PlayerId"
            );

            assertThrows(IllegalArgumentException.class, () ->
                            new AbondanceBid(testPlayerId, null, chosenTrump),
                    "Should reject null BidType"
            );
        }

        @Test
        @DisplayName("Constructor rejects BidTypes outside the ABONDANCE category")
        void constructor_InvalidBidCategory_ThrowsIllegalArgumentException() {
            // MISERIE belongs to the MISERIE category, not ABONDANCE
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    new AbondanceBid(testPlayerId, BidType.MISERIE, chosenTrump)
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
            assertEquals(testPlayerId, bid.getPlayerId(), "getPlayerId() must return the underlying playerId");
            assertEquals(abondanceBidType, bid.getType(), "getType() must return the underlying bidType");
        }

        @Test
        @DisplayName("Native record accessors return correct values")
        void testRecordAccessors() {
            assertEquals(testPlayerId, bid.playerId());
            assertEquals(abondanceBidType, bid.bidType());
            assertEquals(chosenTrump, bid.trump());
        }

        @Test
        @DisplayName("getTeam() always returns only the solo bidder")
        void getTeam_AlwaysReturnsOnlyTheBidder() {
            // Abondance ignores the allBids and allPlayers lists, so empty lists are safe to pass
            List<PlayerId> team = bid.getTeam(Collections.emptyList(), Collections.emptyList());

            assertEquals(1, team.size());
            assertTrue(team.contains(testPlayerId));
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
    @DisplayName("Score Calculation Logic")
    class CalculationTests {

        @ParameterizedTest(name = "Winning with {0} tricks awards positive base points")
        @ValueSource(ints = {9, 10, 13}) // Testing exact target, over target, and max tricks
        void calculateBasePoints_TargetMetOrExceeded_ReturnsPositivePoints(int tricksWon) {
            int expectedPoints = abondanceBidType.getBasePoints();
            assertEquals(expectedPoints, bid.calculateBasePoints(tricksWon));
        }

        @ParameterizedTest(name = "Failing with {0} tricks deducts base points")
        @ValueSource(ints = {8, 4, 0}) // Testing barely failed, severely failed, and 0 tricks
        void calculateBasePoints_BelowTarget_ReturnsNegativePoints(int tricksWon) {
            int expectedPenalty = -1 * abondanceBidType.getBasePoints();
            assertEquals(expectedPenalty, bid.calculateBasePoints(tricksWon));
        }

        @Test
        @DisplayName("calculateBasePoints() rejects negative trick counts")
        void calculateBasePoints_NegativeInput_ThrowsIllegalArgumentException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    bid.calculateBasePoints(-1)
            );
            assertTrue(exception.getMessage().contains("negative"));
        }
    }

    @Nested
    @DisplayName("Auto-Generated Record Methods")
    class RecordImplicitMethodsTests {

        @Test
        @DisplayName("Validates auto-generated equality (equals)")
        void testRecordEquality() {
            AbondanceBid sameBid = new AbondanceBid(testPlayerId, abondanceBidType, chosenTrump);
            AbondanceBid differentBid = new AbondanceBid(new PlayerId(), BidType.ABONDANCE_10, Suit.HEARTS);

            assertEquals(bid, sameBid, "Records with identical data should be equal");
            assertNotEquals(bid, differentBid, "Records with different data should not be equal");
            assertNotEquals(bid, null, "Record should not equal null");
            assertNotEquals(bid, new Object(), "Record should not equal an object of a different type");
        }

        @Test
        @DisplayName("Validates auto-generated hashCode")
        void testRecordHashCode() {
            AbondanceBid sameBid = new AbondanceBid(testPlayerId, abondanceBidType, chosenTrump);
            assertEquals(bid.hashCode(), sameBid.hashCode(), "Equal records should have the same hash code");
        }

        @Test
        @DisplayName("Validates auto-generated toString")
        void testRecordToString() {
            String toStringResult = bid.toString();

            assertTrue(toStringResult.contains("AbondanceBid"), "Should contain the class name");
            assertTrue(toStringResult.contains(testPlayerId.toString()), "Should contain the PlayerId");
            assertTrue(toStringResult.contains(abondanceBidType.toString()), "Should contain the BidType");
            assertTrue(toStringResult.contains(chosenTrump.toString()), "Should contain the Trump suit");
        }
    }
}