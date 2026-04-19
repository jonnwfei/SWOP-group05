package base.domain.trick;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scenario tests for Trick logic, ensuring defensive handling of illegal inputs
 * and correct winning card evaluation[cite: 56, 385].
 */
@DisplayName("Trick Logic")
class TrickTest {

    private static final PlayerId P1 = new PlayerId();
    private static final PlayerId P2 = new PlayerId();
    private static final PlayerId P3 = new PlayerId();
    private static final PlayerId P4 = new PlayerId();

    private Trick trick;

    @BeforeEach
    void setUp() {
        // Initialize with P1 as starter and CLUBS as trump
        trick = new Trick(P1, Suit.CLUBS);
    }

    @Nested
    @DisplayName("Initialization & State")
    class InitializationTests {

        @Test
        @DisplayName("New trick should have zero turns and no winner")
        void shouldInitializeCorrectly() {
            assertTrue(trick.getTurns().isEmpty());
            assertEquals(P1, trick.getStartingPlayerId());
            assertNull(trick.getWinningPlayerId());
            assertNull(trick.getLeadingSuit());
            assertFalse(trick.isCompleted());
        }

        @Test
        @DisplayName("Constructor should throw if starting player is null")
        void constructorShouldThrowOnNullStarter() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new Trick(null, Suit.HEARTS));

            assertTrue(exception.getMessage().contains("Starting player ID must exist"));
        }
    }

    @Nested
    @DisplayName("Turn Management")
    class TurnTests {

        @Test
        @DisplayName("Adding a turn should update turns list and leading suit")
        void shouldRecordTurnAndLeadSuit() {
            Card leadCard = new Card(Suit.HEARTS, Rank.ACE);

            trick.addTurn(P1, leadCard);

            assertEquals(1, trick.getTurns().size());
            assertEquals(Suit.HEARTS, trick.getLeadingSuit());
            assertEquals(P1, trick.getWinningPlayerId());
        }

        @Test
        @DisplayName("Should throw if same player tries to play twice")
        void shouldPreventDuplicatePlayer() {
            trick.addTurn(P1, new Card(Suit.HEARTS, Rank.ACE));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> trick.addTurn(P1, new Card(Suit.HEARTS, Rank.KING)));

            assertTrue(exception.getMessage().contains("already played"));
        }

        @Test
        @DisplayName("Should throw if adding a 5th turn to a full trick")
        void shouldPreventExtraTurns() {
            playFullTrick();

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> trick.addTurn(new PlayerId(), new Card(Suit.DIAMONDS, Rank.TWO)));

            assertTrue(exception.getMessage().contains("already full"));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if playerId or card is null")
        void shouldThrowWhenPlayerIdOrCardIsNull() {
            Card validCard = new Card(Suit.HEARTS, Rank.ACE);
            PlayerId validId = P1;

            // Branch 1: playerId is null
            IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                    () -> trick.addTurn(null, validCard));

            // Branch 2: playedCard is null
            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                    () -> trick.addTurn(validId, null));

            // Verify message consistency
            String expectedMessage = "Trick: PlayerId and Card must exist.";
            assertEquals(expectedMessage, ex1.getMessage());
            assertEquals(expectedMessage, ex2.getMessage());
        }
    }

    @Nested
    @DisplayName("Winner Calculation (Delegation to Evaluator)")
    class WinningLogicTests {

        @Test
        @DisplayName("Higher rank of leading suit should win over lower rank")
        void higherLeadShouldBeatLowerLead() {
            trick.addTurn(P1, new Card(Suit.HEARTS, Rank.TEN));
            trick.addTurn(P2, new Card(Suit.HEARTS, Rank.ACE)); // Higher lead
            trick.addTurn(P3, new Card(Suit.DIAMONDS, Rank.ACE)); // Higher rank but wrong suit

            assertEquals(P2, trick.getWinningPlayerId());
        }

        @Test
        @DisplayName("Any trump card should win over the leading suit")
        void trumpShouldBeatLead() {
            // Trump is CLUBS (set in setUp) [cite: 160]
            trick.addTurn(P1, new Card(Suit.HEARTS, Rank.ACE));
            trick.addTurn(P2, new Card(Suit.CLUBS, Rank.TWO)); // Low Trump beats other suits [cite: 161]

            assertEquals(P2, trick.getWinningPlayerId());
        }

        @Test
        @DisplayName("Higher trump should win over lower trump")
        void higherTrumpShouldBeatLowerTrump() {
            trick.addTurn(P1, new Card(Suit.HEARTS, Rank.ACE));
            trick.addTurn(P2, new Card(Suit.CLUBS, Rank.TWO)); // Low Trump
            trick.addTurn(P3, new Card(Suit.CLUBS, Rank.KING)); // High Trump wins [cite: 161]

            assertEquals(P3, trick.getWinningPlayerId());
        }

        @Test
        @DisplayName("In a No-Trump game (Miserie), lead suit rules apply")
        void shouldHandleNoTrumpGames() {
            // There is no longer a trump suit in Miserie [cite: 186]
            Trick miserieTrick = new Trick(P1, null);

            miserieTrick.addTurn(P1, new Card(Suit.HEARTS, Rank.TEN));
            miserieTrick.addTurn(P2, new Card(Suit.CLUBS, Rank.ACE)); // Would be trump, but isn't here
            miserieTrick.addTurn(P3, new Card(Suit.HEARTS, Rank.JACK)); // Beats the 10 based on lead suit [cite: 162]

            assertEquals(P3, miserieTrick.getWinningPlayerId());
        }
    }

    private void playFullTrick() {
        trick.addTurn(P1, new Card(Suit.HEARTS, Rank.TWO));
        trick.addTurn(P2, new Card(Suit.HEARTS, Rank.THREE));
        trick.addTurn(P3, new Card(Suit.HEARTS, Rank.FOUR));
        trick.addTurn(P4, new Card(Suit.HEARTS, Rank.FIVE));
    }
}