package base.storage.snapshots;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.snapshots.RoundSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundSnapshotTest {

    // Helper method to generate valid score deltas for baseline tests
    private List<Integer> getValidDeltas() {
        return Arrays.asList(90, -30, -30, -30); // Zero-sum
    }

    @Nested
    @DisplayName("Valid Construction Tests")
    class ValidConstruction {

        @Test
        @DisplayName("Successfully creates a valid RoundSnapshot for a normal bid")
        void testValidNormalBid() {
            assertDoesNotThrow(() -> {
                new RoundSnapshot(
                        List.of("1", "2", "3", "4"),
                        BidType.SOLO,
                        0,
                        List.of(0),
                        13,
                        List.of(),
                        1,
                        getValidDeltas(),
                        Suit.HEARTS
                );
            });
        }

        @Test
        @DisplayName("Successfully creates a valid RoundSnapshot for a miserie bid")
        void testValidMiserieBid() {
            assertDoesNotThrow(() -> {
                new RoundSnapshot(
                        List.of("1", "2", "3", "4"),
                        BidType.MISERIE,
                        2,
                        List.of(1, 2),
                        -1,
                        List.of(2),
                        2,
                        Arrays.asList(-30, 15, 15, 0),
                        Suit.HEARTS
                );
            });
        }

        @Test
        @DisplayName("Null miserieWinnerIndices gracefully defaults to empty list")
        void testNullMiserieWinnersDefaultsToEmpty() {
            RoundSnapshot snapshot = new RoundSnapshot(
                    List.of("1", "2", "3", "4"),
                    BidType.SOLO,
                    0,
                    List.of(0),
                    5,
                    null, // Passing null here
                    1,
                    getValidDeltas(),
                    Suit.HEARTS
            );

            assertNotNull(snapshot.miserieWinnerIndices());
            assertTrue(snapshot.miserieWinnerIndices().isEmpty());
        }
    }

    @Nested
    @DisplayName("Basic Field Validation Tests")
    class BasicFieldValidation {

        @Test
        @DisplayName("Rejects null bidType")
        void testNullBidType() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),null, 0, List.of(0), 5, List.of(), 1, getValidDeltas(), Suit.HEARTS)
            );
        }

        @Test
        @DisplayName("Rejects out of bounds bidderIndex")
        void testInvalidBidderIndex() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, -1, List.of(0), 5, List.of(), 1, getValidDeltas(), Suit.HEARTS)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 4, List.of(0), 5, List.of(), 1, getValidDeltas(), Suit.HEARTS)
            );
        }

        @Test
        @DisplayName("Rejects invalid multipliers")
        void testInvalidMultiplier() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0), 5, List.of(), 0, getValidDeltas(), Suit.HEARTS)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0), 5, List.of(), -2, getValidDeltas(), Suit.HEARTS)
            );
        }

        @Test
        @DisplayName("Rejects out of bounds tricksWon")
        void testInvalidTricksWon() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0), -2, List.of(), 1, getValidDeltas(), Suit.HEARTS)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0), 14, List.of(), 1, getValidDeltas(), Suit.HEARTS)
            );
        }
    }

    @Nested
    @DisplayName("List and Index Validation Tests")
    class ListValidation {

        @Test
        @DisplayName("Rejects null or invalid participantIndices")
        void testInvalidParticipantIndices() {
            // Null list
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, null, 5, List.of(), 1, getValidDeltas(), Suit.HEARTS)
            );

            // Contains null element
            List<Integer> listWithNull = new ArrayList<>();
            listWithNull.add(null);
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, listWithNull, 5, List.of(), 1, getValidDeltas(), Suit.HEARTS)
            );

            // Contains out of bounds indices
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(-1), 5, List.of(), 1, getValidDeltas(), Suit.HEARTS)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(4), 5, List.of(), 1, getValidDeltas(), Suit.HEARTS)
            );
        }

        @Test
        @DisplayName("Rejects invalid miserieWinnerIndices")
        void testInvalidMiserieWinnerIndices() {
            // Contains null element
            List<Integer> listWithNull = new ArrayList<>();
            listWithNull.add(null);
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0), 5, listWithNull, 1, getValidDeltas(), Suit.HEARTS)
            );

            // Contains out of bounds index (too low)
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0), 5, List.of(-1), 1, getValidDeltas(), Suit.HEARTS)
            );

            // Contains out of bounds index (too high) - THIS WAS THE MISSING BRANCH!
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0, 1, 2, 3), 5, List.of(4), 1, getValidDeltas(), Suit.HEARTS)
            );
        }

        @Test
        @DisplayName("Rejects miserie winners that are not participants")
        void testMiserieWinnersMustBeParticipants() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.MISERIE, 0, List.of(0, 1), -1, List.of(2), 1, getValidDeltas(), Suit.HEARTS)
            );
        }
    }

    @Nested
    @DisplayName("Score Delta Ledger Tests")
    class ScoreDeltaValidation {

        @Test
        @DisplayName("Rejects null or incorrectly sized scoreDeltas")
        void testInvalidScoreDeltasSize() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0), 5, List.of(), 1, null, Suit.HEARTS)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0), 5, List.of(), 1, List.of(0, 0, 0), Suit.HEARTS) // Size 3
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0), 5, List.of(), 1, List.of(0, 0, 0, 0, 0), Suit.HEARTS) // Size 5
            );
        }

        @Test
        @DisplayName("Rejects null elements inside scoreDeltas")
        void testNullInsideScoreDeltas() {
            List<Integer> deltasWithNull = Arrays.asList(90, -30, null, -60);
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0), 5, List.of(), 1, deltasWithNull, Suit.HEARTS)
            );
        }

        @Test
        @DisplayName("Rejects scoreDeltas that do not sum to exactly zero")
        void testNonZeroSumDeltas() {
            List<Integer> invalidDeltas = Arrays.asList(90, -30, -30, -20); // Sums to +10
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundSnapshot(
                            List.of("1", "2", "3", "4"),BidType.SOLO, 0, List.of(0), 5, List.of(), 1, invalidDeltas, Suit.HEARTS)
            );
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityValidation {

        @Test
        @DisplayName("Ensures internal lists are deep copied and immutable")
        void testListImmutability() {
            List<Integer> participants = new ArrayList<>(Arrays.asList(0, 1));
            List<Integer> miserieWinners = new ArrayList<>(List.of(1));
            List<Integer> deltas = new ArrayList<>(Arrays.asList(30, -10, -10, -10));

            RoundSnapshot snapshot = new RoundSnapshot(
                    List.of("1", "2", "3", "4"),
                    BidType.MISERIE,
                    0,
                    participants,
                    -1,
                    miserieWinners,
                    1,
                    deltas,
                    Suit.HEARTS
            );

            // Attempt to mutate original lists
            participants.add(2);
            miserieWinners.add(0);
            deltas.set(0, 999);

            // Verify the snapshot remained unchanged
            assertEquals(2, snapshot.participantIndices().size(), "Participant list should be isolated from external changes");
            assertEquals(1, snapshot.miserieWinnerIndices().size(), "Miserie winner list should be isolated from external changes");
            assertEquals(30, snapshot.scoreDeltas().getFirst(), "Score deltas should be isolated from external changes");

            // Attempt to mutate the snapshot's lists directly
            assertThrows(UnsupportedOperationException.class, () -> snapshot.participantIndices().add(3));
            assertThrows(UnsupportedOperationException.class, () -> snapshot.miserieWinnerIndices().add(3));
            assertThrows(UnsupportedOperationException.class, () -> snapshot.scoreDeltas().add(3));
        }
    }
}