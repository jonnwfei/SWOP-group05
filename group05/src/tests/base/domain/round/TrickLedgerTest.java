package base.domain.round;

import base.domain.player.PlayerId;
import base.domain.trick.Trick;
import base.domain.round.TrickLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrickLedger Domain Entity Tests")
class TrickLedgerTest {

    private TrickLedger ledger;

    private final PlayerId p1 = new PlayerId();
    private final PlayerId p2 = new PlayerId();
    private final PlayerId p3 = new PlayerId();

    @BeforeEach
    void setUp() {
        ledger = new TrickLedger();
    }

    @Nested
    @DisplayName("Initialization & Empty State")
    class EmptyStateTests {

        @Test
        @DisplayName("Newly created ledger is completely empty")
        void emptyStateBehaviors() {
            assertTrue(ledger.getTricks().isEmpty(), "Tricks list should be empty");
            assertNull(ledger.getLastTrick(), "Last trick should be null when empty");
            assertFalse(ledger.isFull(), "Ledger should not be full initially");
            assertEquals(0, ledger.getTricksWonByTeam(List.of(p1)));
            assertFalse(ledger.hasPlayerWonAnyTrick(p1));
        }

        @Test
        @DisplayName("getTricks returns an unmodifiable copy to protect internal state")
        void getTricksIsImmutable() {
            List<Trick> tricks = ledger.getTricks();
            Trick validTrick = mockValidTrick(p1);

            assertThrows(UnsupportedOperationException.class, () -> tricks.add(validTrick),
                    "Should not be able to modify the internal list directly");
        }
    }

    @Nested
    @DisplayName("Recording Tricks & Validation Guards")
    class RecordingTests {

        @Test
        @DisplayName("Rejects null trick")
        void recordTrick_Null_ThrowsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ledger.recordTrick(null));
            assertTrue(ex.getMessage().contains("must not be null"));
        }

        @Test
        @DisplayName("Rejects trick with null turns list")
        void recordTrick_NullTurns_ThrowsException() {
            Trick invalidTrick = mock(Trick.class);
            when(invalidTrick.getTurns()).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ledger.recordTrick(invalidTrick));
            assertTrue(ex.getMessage().contains("not completed yet"));
        }

        @Test
        @DisplayName("Rejects incomplete trick (less than MAX_TURNS played)")
        void recordTrick_IncompleteTrick_ThrowsException() {
            Trick incompleteTrick = mock(Trick.class);
            when(incompleteTrick.getTurns()).thenReturn(List.of(mock(), mock())); // Only 2 turns

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ledger.recordTrick(incompleteTrick));
            assertTrue(ex.getMessage().contains("not completed yet"));
        }

        @Test
        @DisplayName("Rejects trick with no registered winner")
        void recordTrick_NullWinner_ThrowsException() {
            Trick invalidTrick = mock(Trick.class);
            when(invalidTrick.getTurns()).thenReturn(List.of(mock(), mock(), mock(), mock()));
            when(invalidTrick.getWinningPlayerId()).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ledger.recordTrick(invalidTrick));
            assertTrue(ex.getMessage().contains("must have a winning player"));
        }

        @Test
        @DisplayName("Successfully records a valid, completed trick")
        void recordTrick_Success() {
            Trick validTrick = mockValidTrick(p1);

            ledger.recordTrick(validTrick);

            assertEquals(1, ledger.getTricks().size());
            assertEquals(validTrick, ledger.getLastTrick());
            assertFalse(ledger.isFull());
        }

        @Test
        @DisplayName("Rejects recording when ledger is already full (13 tricks)")
        void recordTrick_LedgerFull_ThrowsException() {
            // Fill the ledger to the MAX_TRICKS limit (13)
            for (int i = 0; i < TrickLedger.MAX_TRICKS; i++) {
                ledger.recordTrick(mockValidTrick(p1));
            }

            assertTrue(ledger.isFull());
            assertEquals(TrickLedger.MAX_TRICKS, ledger.getTricks().size());

            // Attempt to add a 14th trick
            Trick overflowTrick = mockValidTrick(p2);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ledger.recordTrick(overflowTrick));
            assertTrue(ex.getMessage().contains("already finished"));
        }
    }

    @Nested
    @DisplayName("Querying & Analysis Metrics")
    class QueryingTests {

        @Test
        @DisplayName("Rejects null team for getTricksWonByTeam")
        void getTricksWonByTeam_Null_ThrowsException() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> ledger.getTricksWonByTeam(null));
            assertTrue(ex.getMessage().contains("Team cannot be null"));
        }

        @Test
        @DisplayName("Rejects null playerId for hasPlayerWonAnyTrick")
        void hasPlayerWonAnyTrick_Null_ThrowsException() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> ledger.hasPlayerWonAnyTrick(null));
            assertTrue(ex.getMessage().contains("PlayerId cannot be null"));
        }

        @Nested
        @DisplayName("With Populated Ledger")
        class PopulatedLedgerTests {

            @BeforeEach
            void populateLedger() {
                // P1 wins 2 tricks, P2 wins 1 trick, P3 wins 0 tricks
                ledger.recordTrick(mockValidTrick(p1));
                ledger.recordTrick(mockValidTrick(p2));
                ledger.recordTrick(mockValidTrick(p1));
            }

            @Test
            @DisplayName("getTricksWonByTeam correctly aggregates tricks for a team of any size")
            void getTricksWonByTeam() {
                // 1v3 Scenario (Solo)
                assertEquals(2, ledger.getTricksWonByTeam(List.of(p1)), "P1 won exactly 2 tricks");
                assertEquals(1, ledger.getTricksWonByTeam(List.of(p2)), "P2 won exactly 1 trick");
                assertEquals(0, ledger.getTricksWonByTeam(List.of(p3)), "P3 won 0 tricks");

                // 2v2 Scenario (Proposal)
                assertEquals(3, ledger.getTricksWonByTeam(List.of(p1, p2)), "Team P1+P2 won 3 tricks combined");
                assertEquals(2, ledger.getTricksWonByTeam(List.of(p1, p3)), "Team P1+P3 won 2 tricks combined");

                // Empty team edge case
                assertEquals(0, ledger.getTricksWonByTeam(List.of()));
            }

            @Test
            @DisplayName("hasPlayerWonAnyTrick correctly identifies boolean success state for Miserie")
            void hasPlayerWonAnyTrick() {
                assertTrue(ledger.hasPlayerWonAnyTrick(p1), "P1 won a trick");
                assertTrue(ledger.hasPlayerWonAnyTrick(p2), "P2 won a trick");

                // Crucial for Miserie evaluations: P3 successfully dodged all tricks
                assertFalse(ledger.hasPlayerWonAnyTrick(p3), "P3 did not win any tricks");
            }
        }
    }

    // --- Helpers ---

    /**
     * Helper method to generate a valid mock trick that passes the TrickLedger's guards.
     */
    private Trick mockValidTrick(PlayerId winnerId) {
        Trick mockTrick = mock(Trick.class);

        // Simulating Trick.MAX_TURNS (which is statically 4 in Whist)
        // Using mock() items just to fulfill the List.size() requirement
        when(mockTrick.getTurns()).thenReturn(List.of(mock(), mock(), mock(), mock()));

        // We use lenient() here because sometimes we just add tricks to fill the ledger
        // without querying who won them, which would otherwise trigger an UnnecessaryStubbingException
        lenient().when(mockTrick.getWinningPlayerId()).thenReturn(winnerId);

        return mockTrick;
    }
}