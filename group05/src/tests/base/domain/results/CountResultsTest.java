package base.domain.results;

import base.domain.bid.BidType;
import base.domain.player.Player;
import base.domain.round.Round;
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
@DisplayName("CountResults Data Transfer Objects & Validation")
class CountResultsTest {

    @Mock private Player mockPlayer;
    @Mock private Round mockRound;

    private final BidType testBidType = BidType.SOLO;

    @Nested
    @DisplayName("Architectural Integrity")
    class ArchitectureTests {
        @Test
        @DisplayName("CountResults must be a sealed interface with strictly 9 permitted subclasses")
        void verifySealedInterface() {
            assertTrue(CountResults.class.isInterface(), "CountResults should be an interface.");
            assertTrue(CountResults.class.isSealed(), "CountResults must be sealed.");

            List<Class<?>> permitted = Arrays.asList(CountResults.class.getPermittedSubclasses());
            assertEquals(9, permitted.size(), "Should have exactly 9 permitted subclasses.");
            assertTrue(permitted.contains(CountResults.AddPlayerResult.class));
            assertTrue(permitted.contains(CountResults.AddHumanPlayerResult.class));
            assertTrue(permitted.contains(CountResults.BidSelectionResult.class));
            assertTrue(permitted.contains(CountResults.SuitSelectionResult.class));
            assertTrue(permitted.contains(CountResults.PlayerSelectionResult.class));
            assertTrue(permitted.contains(CountResults.AmountOfTrickWonResult.class));
            assertTrue(permitted.contains(CountResults.ScoreBoardResult.class));
            assertTrue(permitted.contains(CountResults.SaveDescriptionResult.class));
            assertTrue(permitted.contains(CountResults.DeleteRoundResult.class));
        }
    }

    @Nested
    @DisplayName("Empty Marker Records")
    class EmptyRecordsTests {
        // Testing all empty marker records in one block for brevity, ensuring 100% coverage on implicit methods

        @Test
        @DisplayName("Validates AddPlayerResult")
        void addPlayerResult() {
            CountResults.AddPlayerResult r1 = new CountResults.AddPlayerResult();
            CountResults.AddPlayerResult r2 = new CountResults.AddPlayerResult();
            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("AddPlayerResult"));
        }

        @Test
        @DisplayName("Validates AddHumanPlayerResult")
        void addHumanPlayerResult() {
            CountResults.AddHumanPlayerResult r1 = new CountResults.AddHumanPlayerResult();
            CountResults.AddHumanPlayerResult r2 = new CountResults.AddHumanPlayerResult();
            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("AddHumanPlayerResult"));
        }

        @Test
        @DisplayName("Validates SuitSelectionResult")
        void suitSelectionResult() {
            CountResults.SuitSelectionResult r1 = new CountResults.SuitSelectionResult();
            CountResults.SuitSelectionResult r2 = new CountResults.SuitSelectionResult();
            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("SuitSelectionResult"));
        }

        @Test
        @DisplayName("Validates AmountOfTrickWonResult")
        void amountOfTrickWonResult() {
            CountResults.AmountOfTrickWonResult r1 = new CountResults.AmountOfTrickWonResult();
            CountResults.AmountOfTrickWonResult r2 = new CountResults.AmountOfTrickWonResult();
            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("AmountOfTrickWonResult"));
        }

        @Test
        @DisplayName("Validates SaveDescriptionResult")
        void saveDescriptionResult() {
            CountResults.SaveDescriptionResult r1 = new CountResults.SaveDescriptionResult();
            CountResults.SaveDescriptionResult r2 = new CountResults.SaveDescriptionResult();
            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("SaveDescriptionResult"));
        }
    }

    @Nested
    @DisplayName("BidSelectionResult Record")
    class BidSelectionResultTests {

        @Test
        @DisplayName("Successfully creates and returns data defensively")
        void creationAndAccessors() {
            BidType[] bids = {testBidType};
            List<Player> players = List.of(mockPlayer);

            CountResults.BidSelectionResult result = new CountResults.BidSelectionResult(bids, players);

            assertArrayEquals(bids, result.availableBids());
            assertEquals(players, result.players());
        }

        @Test
        @DisplayName("Defensively copies arrays and lists to prevent mutation")
        void defensiveCopying() {
            BidType[] mutableBids = {testBidType};
            List<Player> mutablePlayers = new ArrayList<>(List.of(mockPlayer));

            CountResults.BidSelectionResult result = new CountResults.BidSelectionResult(mutableBids, mutablePlayers);

            // Mutate external sources
            mutableBids[0] = BidType.PASS;
            mutablePlayers.clear();

            // Record state must remain completely isolated
            assertEquals(testBidType, result.availableBids()[0]);
            assertFalse(result.players().isEmpty());

            // Accessor defensive copy check:
            // Changing the returned array must NOT change the internal array
            BidType[] accessedArray = result.availableBids();
            accessedArray[0] = BidType.PASS;
            assertEquals(testBidType, result.availableBids()[0], "Accessor must return a cloned array");

            assertThrows(UnsupportedOperationException.class, () -> result.players().clear());
        }

        @Test
        @DisplayName("Rejects null or invalid arguments thoroughly")
        void validationGuards() {
            BidType[] validBids = {testBidType};
            List<Player> validPlayers = List.of(mockPlayer);

            assertThrows(IllegalArgumentException.class, () -> new CountResults.BidSelectionResult(null, validPlayers));
            assertThrows(IllegalArgumentException.class, () -> new CountResults.BidSelectionResult(new BidType[0], validPlayers));
            assertThrows(IllegalArgumentException.class, () -> new CountResults.BidSelectionResult(validBids, null));
            assertThrows(IllegalArgumentException.class, () -> new CountResults.BidSelectionResult(validBids, Collections.emptyList()));
        }

        @Test
        @DisplayName("Validates implicit methods acknowledging native array equality")
        void implicitMethods() {
            BidType[] bids1 = {testBidType};
            BidType[] bids2 = {testBidType}; // Distinct array instance

            CountResults.BidSelectionResult r1 = new CountResults.BidSelectionResult(bids1, List.of(mockPlayer));
            CountResults.BidSelectionResult r1_ref = r1;
            CountResults.BidSelectionResult r2 = new CountResults.BidSelectionResult(bids2, List.of(mockPlayer));

            // Native records evaluate array fields using reference equality (==)
            assertEquals(r1, r1_ref);
            assertEquals(r1.hashCode(), r1_ref.hashCode());
            assertNotEquals(r1, r2, "Different array instances evaluate to false natively");

            assertArrayEquals(r1.availableBids(), r2.availableBids());
            assertTrue(r1.toString().contains("BidSelectionResult"));
        }
    }

    @Nested
    @DisplayName("PlayerSelectionResult Record")
    class PlayerSelectionResultTests {

        @Test
        @DisplayName("Creates successfully using the fully loaded constructor")
        void fullConstructor() {
            List<Player> players = List.of(mockPlayer);
            CountResults.PlayerSelectionResult result = new CountResults.PlayerSelectionResult(players, true, testBidType);

            assertEquals(players, result.players());
            assertTrue(result.multiSelect());
            assertEquals(testBidType, result.type());
        }

        @Test
        @DisplayName("Creates successfully using the overloaded compact constructor")
        void overloadedConstructor() {
            List<Player> players = List.of(mockPlayer);
            CountResults.PlayerSelectionResult result = new CountResults.PlayerSelectionResult(players);

            assertEquals(players, result.players());
            assertFalse(result.multiSelect(), "Should default to false");
            assertEquals(BidType.PASS, result.type(), "Should default to PASS");
        }

        @Test
        @DisplayName("Rejects null or invalid arguments thoroughly")
        void validationGuards() {
            List<Player> validPlayers = List.of(mockPlayer);

            assertThrows(IllegalArgumentException.class, () -> new CountResults.PlayerSelectionResult(null, true, testBidType));
            assertThrows(IllegalArgumentException.class, () -> new CountResults.PlayerSelectionResult(Collections.emptyList(), true, testBidType));
            assertThrows(IllegalArgumentException.class, () -> new CountResults.PlayerSelectionResult(validPlayers, true, null));
        }

        @Test
        @DisplayName("Validates implicit methods")
        void implicitMethods() {
            List<Player> players = List.of(mockPlayer);
            CountResults.PlayerSelectionResult r1 = new CountResults.PlayerSelectionResult(players, true, testBidType);
            CountResults.PlayerSelectionResult r2 = new CountResults.PlayerSelectionResult(players, true, testBidType);
            CountResults.PlayerSelectionResult r3 = new CountResults.PlayerSelectionResult(players, false, testBidType);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertNotEquals(r1, r3);
            assertTrue(r1.toString().contains("PlayerSelectionResult"));
        }
    }

    @Nested
    @DisplayName("ScoreBoardResult Record")
    class ScoreBoardResultTests {

        @Test
        @DisplayName("Creates successfully with valid matching lists")
        void validCreation() {
            List<String> names = List.of("Alice", "Bob");
            List<Integer> scores = List.of(100, -50);

            CountResults.ScoreBoardResult result = new CountResults.ScoreBoardResult(names, scores, true);

            assertEquals(names, result.names());
            assertEquals(scores, result.scores());
            assertTrue(result.canRemovePlayer());
        }

        @Test
        @DisplayName("Exhaustively evaluates every single validation guard branch")
        void validationGuards() {
            List<String> validNames = List.of("Alice");
            List<Integer> validScores = List.of(100);

            // Names list validation
            assertThrows(IllegalArgumentException.class, () -> new CountResults.ScoreBoardResult(null, validScores, true));
            assertThrows(IllegalArgumentException.class, () -> new CountResults.ScoreBoardResult(Collections.emptyList(), validScores, true));

            // Names content validation
            assertThrows(IllegalArgumentException.class, () -> new CountResults.ScoreBoardResult(Arrays.asList("Alice", null), List.of(10, 20), true));
            assertThrows(IllegalArgumentException.class, () -> new CountResults.ScoreBoardResult(List.of("   "), validScores, true));

            // Scores list validation
            assertThrows(IllegalArgumentException.class, () -> new CountResults.ScoreBoardResult(validNames, null, true));
            assertThrows(IllegalArgumentException.class, () -> new CountResults.ScoreBoardResult(validNames, Collections.emptyList(), true));

            // Size matching validation
            assertThrows(IllegalArgumentException.class, () -> new CountResults.ScoreBoardResult(List.of("Alice", "Bob"), validScores, true));
        }

        @Test
        @DisplayName("Defensively copies lists to ensure true immutability")
        void defensiveCopying() {
            List<String> mutableNames = new ArrayList<>(List.of("Alice"));
            List<Integer> mutableScores = new ArrayList<>(List.of(100));

            CountResults.ScoreBoardResult result = new CountResults.ScoreBoardResult(mutableNames, mutableScores, false);

            mutableNames.clear();
            mutableScores.clear();

            assertFalse(result.names().isEmpty());
            assertFalse(result.scores().isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> result.names().clear());
            assertThrows(UnsupportedOperationException.class, () -> result.scores().clear());
        }

        @Test
        @DisplayName("Validates implicit methods")
        void implicitMethods() {
            List<String> names = List.of("Alice");
            List<Integer> scores = List.of(100);

            CountResults.ScoreBoardResult r1 = new CountResults.ScoreBoardResult(names, scores, false);
            CountResults.ScoreBoardResult r2 = new CountResults.ScoreBoardResult(names, scores, false);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("Alice"));
        }
    }

    @Nested
    @DisplayName("DeleteRoundResult Record")
    class DeleteRoundResultTests {

        @Test
        @DisplayName("Validates creation and implicit methods")
        void creationAndImplicitMethods() {
            List<Round> rounds = List.of(mockRound);

            CountResults.DeleteRoundResult r1 = new CountResults.DeleteRoundResult(rounds);
            CountResults.DeleteRoundResult r2 = new CountResults.DeleteRoundResult(rounds);
            CountResults.DeleteRoundResult r3 = new CountResults.DeleteRoundResult(Collections.emptyList());

            assertEquals(rounds, r1.rounds());
            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertNotEquals(r1, r3);
            assertTrue(r1.toString().contains("DeleteRoundResult"));
        }
    }
}