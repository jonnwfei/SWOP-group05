package base.domain.results;

import base.domain.bid.BidType;
import base.domain.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the BidSelectionResult record, focusing on defensive programming
 * and validation of game state data.
 */
@DisplayName("Bid Selection Result Tests")
class BidSelectionResultTest {

    @Mock
    private Player mockPlayer1;

    @Mock
    private Player mockPlayer2;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    private BidType[] validBids() {
        return new BidType[]{BidType.PASS, BidType.SOLO};
    }

    @Nested
    @DisplayName("Constructor & Validation")
    class ConstructorTests {

        @Test
        @DisplayName("Should successfully create result with valid inputs and immutable list")
        void shouldAcceptValidInputs() {
            List<Player> players = List.of(mockPlayer1, mockPlayer2);
            BidType[] bids = validBids();

            BidSelectionResult result = new BidSelectionResult(bids, players);

            // Assert
            assertNotNull(result);
            assertArrayEquals(new BidType[]{BidType.PASS, BidType.SOLO}, result.availableBids());
            assertEquals(2, result.players().size());
            assertTrue(result.players().contains(mockPlayer1));
            assertTrue(result.players().contains(mockPlayer2));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if availableBids is null or empty")
        void shouldRejectInvalidBids() {
            List<Player> players = List.of(mockPlayer1);

            // Negative scenarios: verify defensive handling of illegal input
            assertThrows(IllegalArgumentException.class, () -> new BidSelectionResult(null, players));
            assertThrows(IllegalArgumentException.class, () -> new BidSelectionResult(new BidType[]{}, players));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if players list is null or empty")
        void shouldRejectInvalidPlayers() {
            BidType[] bids = validBids();

            assertThrows(IllegalArgumentException.class, () -> new BidSelectionResult(bids, null));
            assertThrows(IllegalArgumentException.class, () -> new BidSelectionResult(bids, List.of()));
        }
    }

    @Nested
    @DisplayName("Defensive Copying & Encapsulation")
    class MutabilityTests {

        @Test
        @DisplayName("Internal array should be protected from external modification of the input array")
        void shouldProtectAgainstInputMutation() {
            BidType[] inputBids = {BidType.PASS};
            BidSelectionResult result = new BidSelectionResult(inputBids, List.of(mockPlayer1));

            // Act: Mutate the array we passed in (External mutation attempt)
            inputBids[0] = BidType.ABONDANCE_10;

            // Assert: Internal state should remain PASS due to defensive cloning [cite: 49]
            assertArrayEquals(new BidType[]{BidType.PASS}, result.availableBids());
        }

        @Test
        @DisplayName("Internal array should be protected from modification via the accessor")
        void shouldProtectAgainstAccessorMutation() {
            BidSelectionResult result = new BidSelectionResult(validBids(), List.of(mockPlayer1));

            // Act: Mutate the array we got back from the getter
            BidType[] externalBids = result.availableBids();
            externalBids[0] = BidType.ABONDANCE_9;

            // Assert: Internal state remains PASS [cite: 49]
            assertEquals(BidType.PASS, result.availableBids()[0]);
        }

        @Test
        @DisplayName("Players list should be an unmodifiable copy")
        void shouldReturnUnmodifiablePlayersList() {
            BidSelectionResult result = new BidSelectionResult(validBids(), List.of(mockPlayer1));

            // Verify unmodifiable collection [cite: 48]
            assertThrows(UnsupportedOperationException.class, () -> result.players().add(mockPlayer2));
        }
    }

    @Nested
    @DisplayName("Record Property Overrides")
    class PropertyTests {

        @Test
        @DisplayName("toString should clearly show field contents")
        void shouldHaveDescriptiveToString() {
            BidSelectionResult result = new BidSelectionResult(new BidType[]{BidType.PASS}, List.of(mockPlayer1));
            String output = result.toString();

            assertTrue(output.contains("BidSelectionResult"));
            assertTrue(output.contains("availableBids"));
            assertTrue(output.contains("players"));
        }
    }
}