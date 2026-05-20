package base.domain.scores;

import base.domain.bid.BidType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScoringRegistry Domain Service Tests")
class ScoringRegistryTest {

    private ScoringRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ScoringRegistry();
    }

    @Nested
    @DisplayName("Initialization & Default Configurations")
    class InitializationTests {

        @Test
        @DisplayName("Successfully populates all standard scorable bids on creation")
        void initializesWithDefaultValues() {
            Map<BidType, ScoringParameters> allParams = registry.getAllParameters();

            // Verify core bids exist
            assertTrue(allParams.containsKey(BidType.PROPOSAL));
            assertTrue(allParams.containsKey(BidType.SOLO));
            assertTrue(allParams.containsKey(BidType.MISERIE));

            // Verify a specific default mathematical configuration (e.g., Solo is 75 pts)
            ScoringParameters soloParams = registry.getParameters(BidType.SOLO);
            assertEquals(75, soloParams.calculatePoints(13));
        }

        @Test
        @DisplayName("getAllParameters returns a strictly unmodifiable map")
        void getAllParameters_IsImmutable() {
            Map<BidType, ScoringParameters> snapshot = registry.getAllParameters();
            ScoringParameters dummyParams = new ScoringParameters(1, 13, 10, 0, false);

            assertThrows(UnsupportedOperationException.class,
                    () -> snapshot.put(BidType.SOLO, dummyParams),
                    "The exposed map snapshot must be immutable to prevent external tampering.");
        }
    }

    @Nested
    @DisplayName("Querying Parameters (getParameters)")
    class QueryTests {

        @Test
        @DisplayName("Successfully fetches existing parameters")
        void getParameters_Success() {
            ScoringParameters params = registry.getParameters(BidType.MISERIE);
            assertNotNull(params);
            assertEquals(0, params.minTricks()); // Miserie requires 0 tricks
        }

        @Test
        @DisplayName("Rejects null BidType queries")
        void getParameters_Null_ThrowsException() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> registry.getParameters(null));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }

        @Test
        @DisplayName("Explicitly rejects querying PASS bids")
        void getParameters_Pass_ThrowsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registry.getParameters(BidType.PASS));
            assertTrue(ex.getMessage().contains("does not have scoring parameters"));
        }
    }

    @Nested
    @DisplayName("Updating Parameters (Use Case 4.9)")
    class UpdateTests {

        @Test
        @DisplayName("Successfully overwrites existing parameters and makes them immediately available")
        void updateParameters_Success() {
            // Setup: Create a custom hardcore Solo configuration (100 base points instead of 75)
            ScoringParameters customSolo = new ScoringParameters(13, 13, 100, 0, false);

            // Execute
            registry.updateParameters(BidType.SOLO, customSolo);

            // Verify
            ScoringParameters fetched = registry.getParameters(BidType.SOLO);
            assertEquals(100, fetched.calculatePoints(13), "The registry should serve the newly updated rules.");
        }

        @Test
        @DisplayName("Rejects null BidType or null Parameters during update")
        void updateParameters_NullGuards() {
            ScoringParameters validParams = new ScoringParameters(1, 13, 10, 0, false);

            assertThrows(NullPointerException.class, () -> registry.updateParameters(null, validParams));
            assertThrows(NullPointerException.class, () -> registry.updateParameters(BidType.SOLO, null));
        }

        @Test
        @DisplayName("Explicitly rejects assigning scoring parameters to PASS bids")
        void updateParameters_Pass_ThrowsException() {
            ScoringParameters validParams = new ScoringParameters(1, 13, 10, 0, false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> registry.updateParameters(BidType.PASS, validParams));
            assertTrue(ex.getMessage().contains("does not have scoring parameters"));
        }
    }
}