package base.domain.results;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.trick.Trick;
import base.domain.turn.PlayTurn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayResults Data Transfer Objects & Validation")
class PlayResultsTest {

    @Mock private Player mockPlayer;
    @Mock private Trick mockTrick;

    // Use REAL instances for Records/Finals to prevent Mockito proxy crashes
    private final PlayerId testPlayerId = new PlayerId();
    private final Card testCard = new Card(Suit.HEARTS, Rank.ACE);
    private final PlayTurn testTurn = new PlayTurn(testPlayerId, testCard);

    @Nested
    @DisplayName("Architectural Integrity")
    class ArchitectureTests {
        @Test
        @DisplayName("PlayResults must be a sealed interface with exactly 6 permitted subclasses")
        void verifySealedInterface() {
            assertTrue(PlayResults.class.isInterface(), "PlayResults should be an interface.");
            assertTrue(PlayResults.class.isSealed(), "PlayResults must be sealed.");

            List<Class<?>> permitted = Arrays.asList(PlayResults.class.getPermittedSubclasses());
            assertEquals(6, permitted.size(), "Should have exactly 6 permitted subclasses.");
            assertTrue(permitted.contains(PlayResults.PlayCardResult.class));
            assertTrue(permitted.contains(PlayResults.EndOfTurnResult.class));
            assertTrue(permitted.contains(PlayResults.EndOfTrickResult.class));
            assertTrue(permitted.contains(PlayResults.EndOfRoundResult.class));
            assertTrue(permitted.contains(PlayResults.TrickHistoryResult.class));
            assertTrue(permitted.contains(PlayResults.ParticipatingPlayersResult.class));
        }
    }

    @Nested
    @DisplayName("PlayCardResult Record")
    class PlayCardResultTests {

        private final List<PlayTurn> validTurns = List.of(testTurn);
        private final List<String> validNames = List.of("Alice");
        private final List<List<Card>> validHands = List.of(List.of(testCard));
        private final List<Card> validLegalCards = List.of(testCard);
        private final Map<PlayerId, String> validMap = Map.of(testPlayerId, "Alice");

        @Test
        @DisplayName("Successfully creates and returns state via accessors")
        void validCreation() {
            PlayResults.PlayCardResult result = new PlayResults.PlayCardResult(
                    validTurns, true, validNames, validHands, 1, mockPlayer, validLegalCards, mockTrick, validMap
            );

            assertEquals(validTurns, result.turns());
            assertTrue(result.isOpenMiserie());
            assertEquals(validNames, result.exposedPlayerNames());
            assertEquals(validHands, result.formattedExposedHand());
            assertEquals(1, result.trickNumber());
            assertEquals(mockPlayer, result.player());
            assertEquals(validLegalCards, result.legalCards());
            assertEquals(mockTrick, result.lastPlayedTrick());
            assertEquals(validMap, result.playerNames());
        }

        @Test
        @DisplayName("Defensively copies lists and maps to ensure immutability")
        void defensiveCopying() {
            List<PlayTurn> mutableTurns = new ArrayList<>(validTurns);
            List<String> mutableNames = new ArrayList<>(validNames);
            List<List<Card>> mutableHands = new ArrayList<>();
            mutableHands.add(new ArrayList<>(List.of(testCard)));
            List<Card> mutableLegalCards = new ArrayList<>(validLegalCards);
            Map<PlayerId, String> mutableMap = new HashMap<>(validMap);

            PlayResults.PlayCardResult result = new PlayResults.PlayCardResult(
                    mutableTurns, false, mutableNames, mutableHands, 1, mockPlayer, mutableLegalCards, null, mutableMap
            );

            // Mutate originals
            mutableTurns.clear();
            mutableNames.clear();
            mutableHands.get(0).clear();
            mutableHands.clear();
            mutableLegalCards.clear();
            mutableMap.clear();

            // Record state must remain fully populated
            assertFalse(result.turns().isEmpty());
            assertFalse(result.exposedPlayerNames().isEmpty());
            assertFalse(result.formattedExposedHand().isEmpty());
            assertFalse(result.formattedExposedHand().get(0).isEmpty());
            assertFalse(result.legalCards().isEmpty());
            assertFalse(result.playerNames().isEmpty());

            // Returned collections must throw exceptions on mutation
            assertThrows(UnsupportedOperationException.class, () -> result.turns().clear());
            assertThrows(UnsupportedOperationException.class, () -> result.formattedExposedHand().get(0).clear());
            assertThrows(UnsupportedOperationException.class, () -> result.playerNames().clear());
        }

        @Test
        @DisplayName("Exhaustively evaluates every single validation guard branch")
        void validationGuards() {
            // Turns
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(null, false, validNames, validHands, 1, mockPlayer, validLegalCards, mockTrick, validMap));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(Arrays.asList(testTurn, null), false, validNames, validHands, 1, mockPlayer, validLegalCards, mockTrick, validMap));

            // Exposed Names
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, null, validHands, 1, mockPlayer, validLegalCards, mockTrick, validMap));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, Arrays.asList("Alice", null), validHands, 1, mockPlayer, validLegalCards, mockTrick, validMap));

            // Formatted Hands
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, validNames, null, 1, mockPlayer, validLegalCards, mockTrick, validMap));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, Arrays.asList("Alice", "Bob"), Arrays.asList(validHands.get(0), null), 1, mockPlayer, validLegalCards, mockTrick, validMap));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, validNames, List.of(Arrays.asList(testCard, null)), 1, mockPlayer, validLegalCards, mockTrick, validMap));

            // Size Mismatch
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, List.of("Alice", "Bob"), validHands, 1, mockPlayer, validLegalCards, mockTrick, validMap));

            // Trick Number
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, validNames, validHands, 0, mockPlayer, validLegalCards, mockTrick, validMap));

            // Player
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, validNames, validHands, 1, null, validLegalCards, mockTrick, validMap));

            // Legal Cards (Custom guard for null, native NullPointerException from List.copyOf if containing nulls)
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, validNames, validHands, 1, mockPlayer, null, mockTrick, validMap));
            assertThrows(NullPointerException.class, () -> new PlayResults.PlayCardResult(validTurns, false, validNames, validHands, 1, mockPlayer, Arrays.asList(testCard, null), mockTrick, validMap));

            // Map (Null key or null value via HashMap since Map.of rejects nulls immediately)
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, validNames, validHands, 1, mockPlayer, validLegalCards, mockTrick, null));

            Map<PlayerId, String> nullKeyMap = new HashMap<>(); nullKeyMap.put(null, "Alice");
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, validNames, validHands, 1, mockPlayer, validLegalCards, mockTrick, nullKeyMap));

            Map<PlayerId, String> nullValMap = new HashMap<>(); nullValMap.put(testPlayerId, null);
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.PlayCardResult(validTurns, false, validNames, validHands, 1, mockPlayer, validLegalCards, mockTrick, nullValMap));
        }

        @Test
        @DisplayName("Validates implicit record methods")
        void implicitMethods() {
            PlayResults.PlayCardResult r1 = new PlayResults.PlayCardResult(validTurns, false, validNames, validHands, 1, mockPlayer, validLegalCards, mockTrick, validMap);
            PlayResults.PlayCardResult r2 = new PlayResults.PlayCardResult(validTurns, false, validNames, validHands, 1, mockPlayer, validLegalCards, mockTrick, validMap);
            PlayResults.PlayCardResult r3 = new PlayResults.PlayCardResult(validTurns, false, validNames, validHands, 2, mockPlayer, validLegalCards, mockTrick, validMap);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertNotEquals(r1, r3);
            assertTrue(r1.toString().contains("Alice"));
        }
    }

    @Nested
    @DisplayName("EndOfTurnResult Record")
    class EndOfTurnResultTests {

        @Test
        @DisplayName("Validates creation and exception guards")
        void creationAndGuards() {
            PlayResults.EndOfTurnResult r = new PlayResults.EndOfTurnResult("Alice", testCard);
            assertEquals("Alice", r.name());
            assertEquals(testCard, r.card());

            assertThrows(IllegalArgumentException.class, () -> new PlayResults.EndOfTurnResult(null, testCard));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.EndOfTurnResult("  ", testCard));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.EndOfTurnResult("Alice", null));
        }

        @Test
        @DisplayName("Validates implicit methods")
        void implicitMethods() {
            PlayResults.EndOfTurnResult r1 = new PlayResults.EndOfTurnResult("Alice", testCard);
            PlayResults.EndOfTurnResult r2 = new PlayResults.EndOfTurnResult("Alice", testCard);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("Alice"));
        }
    }

    @Nested
    @DisplayName("EndOfTrickResult Record")
    class EndOfTrickResultTests {

        @Test
        @DisplayName("Validates creation and exception guards")
        void creationAndGuards() {
            PlayResults.EndOfTrickResult r = new PlayResults.EndOfTrickResult("Alice", testCard, "Bob");
            assertEquals("Alice", r.name());
            assertEquals(testCard, r.card());
            assertEquals("Bob", r.winner());

            assertThrows(IllegalArgumentException.class, () -> new PlayResults.EndOfTrickResult(null, testCard, "Bob"));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.EndOfTrickResult("  ", testCard, "Bob"));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.EndOfTrickResult("Alice", null, "Bob"));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.EndOfTrickResult("Alice", testCard, null));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.EndOfTrickResult("Alice", testCard, "   "));
        }

        @Test
        @DisplayName("Validates implicit methods")
        void implicitMethods() {
            PlayResults.EndOfTrickResult r1 = new PlayResults.EndOfTrickResult("Alice", testCard, "Bob");
            PlayResults.EndOfTrickResult r2 = new PlayResults.EndOfTrickResult("Alice", testCard, "Bob");

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("Bob"));
        }
    }

    @Nested
    @DisplayName("EndOfRoundResult Record")
    class EndOfRoundResultTests {

        @Test
        @DisplayName("Validates creation and exception guards")
        void creationAndGuards() {
            PlayResults.EndOfRoundResult r = new PlayResults.EndOfRoundResult("Alice", testCard);

            assertThrows(IllegalArgumentException.class, () -> new PlayResults.EndOfRoundResult(null, testCard));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.EndOfRoundResult("  ", testCard));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.EndOfRoundResult("Alice", null));
        }

        @Test
        @DisplayName("Validates implicit methods")
        void implicitMethods() {
            PlayResults.EndOfRoundResult r1 = new PlayResults.EndOfRoundResult("Alice", testCard);
            PlayResults.EndOfRoundResult r2 = new PlayResults.EndOfRoundResult("Alice", testCard);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("EndOfRoundResult"));
        }
    }

    @Nested
    @DisplayName("TrickHistoryResult Record")
    class TrickHistoryResultTests {

        @Test
        @DisplayName("Successfully creates, guards invalid states, and ensures defensive mapping")
        void creationAndGuards() {
            Map<PlayerId, String> validMap = Map.of(testPlayerId, "Alice");
            PlayResults.TrickHistoryResult r = new PlayResults.TrickHistoryResult(mockTrick, validMap);
            assertEquals(mockTrick, r.trick());

            // Defensive copy test
            Map<PlayerId, String> mutableMap = new HashMap<>(validMap);
            PlayResults.TrickHistoryResult defensiveResult = new PlayResults.TrickHistoryResult(mockTrick, mutableMap);
            mutableMap.clear();
            assertFalse(defensiveResult.playerNames().isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> defensiveResult.playerNames().clear());

            // Null checks
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.TrickHistoryResult(null, validMap));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.TrickHistoryResult(mockTrick, null));

            // Map content checks via HashMap
            Map<PlayerId, String> nullKeyMap = new HashMap<>(); nullKeyMap.put(null, "Alice");
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.TrickHistoryResult(mockTrick, nullKeyMap));

            Map<PlayerId, String> nullValMap = new HashMap<>(); nullValMap.put(testPlayerId, null);
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.TrickHistoryResult(mockTrick, nullValMap));
        }

        @Test
        @DisplayName("Validates implicit methods")
        void implicitMethods() {
            Map<PlayerId, String> validMap = Map.of(testPlayerId, "Alice");
            PlayResults.TrickHistoryResult r1 = new PlayResults.TrickHistoryResult(mockTrick, validMap);
            PlayResults.TrickHistoryResult r2 = new PlayResults.TrickHistoryResult(mockTrick, validMap);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("Alice"));
        }
    }

    @Nested
    @DisplayName("ParticipatingPlayersResult Record")
    class ParticipatingPlayersResultTests {

        @Test
        @DisplayName("Creates, copies defensively, and guards invalid lists")
        void creationAndGuards() {
            List<String> names = List.of("Alice", "Bob");
            PlayResults.ParticipatingPlayersResult r = new PlayResults.ParticipatingPlayersResult(names);
            assertEquals(names, r.playerNames());

            // Defensive copy test
            List<String> mutableNames = new ArrayList<>(names);
            PlayResults.ParticipatingPlayersResult defensiveResult = new PlayResults.ParticipatingPlayersResult(mutableNames);
            mutableNames.clear();
            assertFalse(defensiveResult.playerNames().isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> defensiveResult.playerNames().clear());

            // Null & Empty
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.ParticipatingPlayersResult(null));
            assertThrows(IllegalArgumentException.class, () -> new PlayResults.ParticipatingPlayersResult(new ArrayList<>()));
        }

        @Test
        @DisplayName("Validates implicit methods")
        void implicitMethods() {
            List<String> names = List.of("Alice", "Bob");
            PlayResults.ParticipatingPlayersResult r1 = new PlayResults.ParticipatingPlayersResult(names);
            PlayResults.ParticipatingPlayersResult r2 = new PlayResults.ParticipatingPlayersResult(names);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
            assertTrue(r1.toString().contains("ParticipatingPlayersResult"));
        }
    }
}