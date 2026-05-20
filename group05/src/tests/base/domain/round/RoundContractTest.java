package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.PlayerId;
import base.domain.scores.ScoringParameters;
import base.domain.scores.ScoringRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoundContract Value Object Tests")
class RoundContractTest {

    @Mock private TrickLedger ledger;
    @Mock private ScoringRegistry registry;
    @Mock private Bid mockBid;
    @Mock private ScoringParameters params;

    private final PlayerId p1 = new PlayerId();
    private final PlayerId p2 = new PlayerId();
    private final PlayerId p3 = new PlayerId();
    private final PlayerId p4 = new PlayerId();

    @Nested
    @DisplayName("Defensive Programming & Validation")
    class ValidationTests {

        @Test
        @DisplayName("Rejects null arguments in constructor")
        void rejectsNullConstructorArguments() {
            List<PlayerId> validTeam1 = List.of(p1);
            List<PlayerId> validTeam2 = List.of(p2, p3, p4);

            assertThrows(NullPointerException.class, () -> new RoundContract(null, Suit.HEARTS, validTeam1, validTeam2, 1));
            assertThrows(NullPointerException.class, () -> new RoundContract(mockBid, Suit.HEARTS, null, validTeam2, 1));
            assertThrows(NullPointerException.class, () -> new RoundContract(mockBid, Suit.HEARTS, validTeam1, null, 1));

            // Note: trumpSuit CAN be null (e.g., for Miserie), so we don't test for that throwing.
            assertDoesNotThrow(() -> new RoundContract(mockBid, null, validTeam1, validTeam2, 1));
        }

        @Test
        @DisplayName("Rejects empty teams or incorrect total player count")
        void enforcesTeamSizeInvariants() {
            List<PlayerId> emptyTeam = List.of();
            List<PlayerId> team1 = List.of(p1);
            List<PlayerId> team2 = List.of(p2, p3); // Total = 3 players, should be 4
            List<PlayerId> team4 = List.of(p1, p2, p3, p4);

            assertThrows(IllegalArgumentException.class, () -> new RoundContract(mockBid, Suit.HEARTS, emptyTeam, team4, 1));
            assertThrows(IllegalArgumentException.class, () -> new RoundContract(mockBid, Suit.HEARTS, team4, emptyTeam, 1));
            assertThrows(IllegalArgumentException.class, () -> new RoundContract(mockBid, Suit.HEARTS, team1, team2, 1),
                    "Contract must strictly enforce 4 players total");
        }

        @Test
        @DisplayName("Rejects invalid multipliers")
        void rejectsInvalidMultiplier() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundContract(mockBid, Suit.HEARTS, List.of(p1), List.of(p2, p3, p4), 0));
            assertThrows(IllegalArgumentException.class, () ->
                    new RoundContract(mockBid, Suit.HEARTS, List.of(p1), List.of(p2, p3, p4), -1));
        }

        @Test
        @DisplayName("Enforces deep immutability on team lists")
        void enforcesListImmutability() {
            List<PlayerId> mutableAttackers = new ArrayList<>(List.of(p1));
            List<PlayerId> mutableDefenders = new ArrayList<>(List.of(p2, p3, p4));

            RoundContract contract = new RoundContract(mockBid, Suit.HEARTS, mutableAttackers, mutableDefenders, 1);

            // Mutate the original lists
            mutableAttackers.add(p2);
            mutableDefenders.clear();

            // The contract should be unaffected
            assertEquals(1, contract.biddingTeam().size());
            assertEquals(3, contract.defendingTeam().size());

            // Returned lists should be unmodifiable
            assertThrows(UnsupportedOperationException.class, () -> contract.biddingTeam().add(p2));
        }

        @Test
        @DisplayName("Rejects null evaluation inputs")
        void rejectsNullEvaluationInputs() {
            RoundContract contract = new RoundContract(mockBid, Suit.HEARTS, List.of(p1), List.of(p2, p3, p4), 1);

            assertThrows(NullPointerException.class, () -> contract.evaluateOutcome(null, registry));
            assertThrows(NullPointerException.class, () -> contract.evaluateOutcome(ledger, null));
        }
    }

    @Nested
    @DisplayName("Record Initialization")
    class InitializationTests {

        @Test
        @DisplayName("Correctly stores all properties as an immutable record")
        void recordHoldsProperties() {
            List<PlayerId> attackers = List.of(p1);
            List<PlayerId> defenders = List.of(p2, p3, p4);

            RoundContract contract = new RoundContract(mockBid, Suit.HEARTS, attackers, defenders, 2);

            assertEquals(mockBid, contract.winningBid());
            assertEquals(Suit.HEARTS, contract.trumpSuit());
            assertEquals(attackers, contract.biddingTeam());
            assertEquals(defenders, contract.defendingTeam());
            assertEquals(2, contract.multiplier());
        }
    }

    @Nested
    @DisplayName("Standard Contract Evaluations (1v3 & 2v2)")
    class StandardEvaluationTests {

        @BeforeEach
        void setupStandardBid() {
            // Assume SOLO is a standard category bid
            when(mockBid.getType()).thenReturn(BidType.SOLO);
            when(registry.getParameters(BidType.SOLO)).thenReturn(params);
        }

        @Test
        @DisplayName("1v3 Win: Bidder gains base points, defenders split inverse")
        void evaluate1v3_Win() {
            RoundContract contract = new RoundContract(mockBid, Suit.HEARTS, List.of(p1), List.of(p2, p3, p4), 1);

            when(ledger.getTricksWonByTeam(List.of(p1))).thenReturn(13);
            when(params.calculatePoints(13)).thenReturn(75); // 75 base points

            Map<PlayerId, Integer> deltas = contract.evaluateOutcome(ledger, registry);

            assertEquals(75, deltas.get(p1));
            assertEquals(-25, deltas.get(p2));
            assertEquals(-25, deltas.get(p3));
            assertEquals(-25, deltas.get(p4));
            assertZeroSum(deltas);
        }

        @Test
        @DisplayName("1v3 Loss with Multiplier: Applies multiplier correctly to all deltas")
        void evaluate1v3_Loss_WithMultiplier() {
            RoundContract contract = new RoundContract(mockBid, Suit.HEARTS, List.of(p1), List.of(p2, p3, p4), 2); // Multiplier = 2

            when(ledger.getTricksWonByTeam(List.of(p1))).thenReturn(0);
            when(params.calculatePoints(0)).thenReturn(-75); // Failed SOLO

            Map<PlayerId, Integer> deltas = contract.evaluateOutcome(ledger, registry);

            // Base = -75 * 2 = -150. Defenders get +150 / 3 = +50.
            assertEquals(-150, deltas.get(p1));
            assertEquals(50, deltas.get(p2));
            assertEquals(50, deltas.get(p3));
            assertEquals(50, deltas.get(p4));
            assertZeroSum(deltas);
        }

        @Test
        @DisplayName("2v2 Win: Bidders both gain full points, defenders both lose full points")
        void evaluate2v2_Win() {
            // Assume PROPOSAL is used for 2v2
            when(mockBid.getType()).thenReturn(BidType.PROPOSAL);
            when(registry.getParameters(BidType.PROPOSAL)).thenReturn(params);

            RoundContract contract = new RoundContract(mockBid, Suit.SPADES, List.of(p1, p2), List.of(p3, p4), 1);

            when(ledger.getTricksWonByTeam(List.of(p1, p2))).thenReturn(8);
            when(params.calculatePoints(8)).thenReturn(2); // 2 base points for normal proposal win

            Map<PlayerId, Integer> deltas = contract.evaluateOutcome(ledger, registry);

            assertEquals(2, deltas.get(p1));
            assertEquals(2, deltas.get(p2));
            assertEquals(-2, deltas.get(p3));
            assertEquals(-2, deltas.get(p4));
            assertZeroSum(deltas);
        }
    }

    @Nested
    @DisplayName("Miserie Contract Evaluations")
    class MiserieEvaluationTests {

        @BeforeEach
        void setupMiserieBid() {
            when(mockBid.getType()).thenReturn(BidType.MISERIE);
            when(registry.getParameters(BidType.MISERIE)).thenReturn(params);
        }

        @Test
        @DisplayName("Single Miserie Win: Bidder gains points, 3 opponents lose inverse")
        void evaluateSingleMiserie_Win() {
            RoundContract contract = new RoundContract(mockBid, null, List.of(p1), List.of(p2, p3, p4), 1);

            when(ledger.hasPlayerWonAnyTrick(p1)).thenReturn(false); // Succeeded
            when(params.calculatePoints(0)).thenReturn(21); // 0 translates to success points

            Map<PlayerId, Integer> deltas = contract.evaluateOutcome(ledger, registry);

            assertEquals(21, deltas.get(p1));
            assertEquals(-7, deltas.get(p2));
            assertEquals(-7, deltas.get(p3));
            assertEquals(-7, deltas.get(p4));
            assertZeroSum(deltas);
        }

        @Test
        @DisplayName("Double Miserie Mixed: One wins, one loses. Matrix aggregates correctly.")
        void evaluateDoubleMiserie_MixedResults() {
            // P1 and P2 both play Miserie
            RoundContract contract = new RoundContract(mockBid, null, List.of(p1, p2), List.of(p3, p4), 1);

            // P1 succeeds (0 tricks), P2 fails (1+ tricks)
            when(ledger.hasPlayerWonAnyTrick(p1)).thenReturn(false);
            when(ledger.hasPlayerWonAnyTrick(p2)).thenReturn(true);

            when(params.calculatePoints(0)).thenReturn(21);  // Win
            when(params.calculatePoints(1)).thenReturn(-21); // Loss

            Map<PlayerId, Integer> deltas = contract.evaluateOutcome(ledger, registry);

            /* Math Breakdown:
               P1's Game (Win):  P1=+21, P2=-7,  P3=-7, P4=-7
               P2's Game (Loss): P2=-21, P1=+7,  P3=+7, P4=+7
               ----------------------------------------------
               Totals:           P1=+28, P2=-28, P3=0,  P4=0
            */
            assertEquals(28, deltas.get(p1), "P1 gets +21 from their win, +7 from P2's loss");
            assertEquals(-28, deltas.get(p2), "P2 gets -21 from their loss, -7 from P1's win");
            assertEquals(0, deltas.get(p3), "Defenders break even (+7 and -7)");
            assertEquals(0, deltas.get(p4), "Defenders break even (+7 and -7)");
            assertZeroSum(deltas);
        }

        @Test
        @DisplayName("Double Miserie Win with Multiplier: Matrix applies multiplier to all aggregates")
        void evaluateDoubleMiserie_DoubleWin_WithMultiplier() {
            RoundContract contract = new RoundContract(mockBid, null, List.of(p1, p2), List.of(p3, p4), 2); // Multiplier = 2

            when(ledger.hasPlayerWonAnyTrick(p1)).thenReturn(false);
            when(ledger.hasPlayerWonAnyTrick(p2)).thenReturn(false);
            when(params.calculatePoints(0)).thenReturn(21);

            Map<PlayerId, Integer> deltas = contract.evaluateOutcome(ledger, registry);

            /* Math Breakdown with 2x Multiplier:
               P1's Game (Win 42): P1=+42, P2=-14, P3=-14, P4=-14
               P2's Game (Win 42): P2=+42, P1=-14, P3=-14, P4=-14
               ----------------------------------------------
               Totals:             P1=+28, P2=+28, P3=-28, P4=-28
            */
            assertEquals(28, deltas.get(p1));
            assertEquals(28, deltas.get(p2));
            assertEquals(-28, deltas.get(p3));
            assertEquals(-28, deltas.get(p4));
            assertZeroSum(deltas);
        }
    }

    // --- Helpers ---

    /**
     * Helper to assert that the sum of all points in the outcome is exactly 0.
     * This guarantees the Whist strict zero-sum constraint is never violated.
     */
    private void assertZeroSum(Map<PlayerId, Integer> deltas) {
        int sum = deltas.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(0, sum, "The sum of all score deltas must be exactly 0 (Zero-Sum Game)");
    }
}