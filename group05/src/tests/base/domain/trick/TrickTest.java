package base.domain.trick;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Trick Logic")
class TrickTest {

    private static final PlayerId P1 = new PlayerId("p1");
    private static final PlayerId P2 = new PlayerId("p2");
    private static final PlayerId P3 = new PlayerId("p3");
    private static final PlayerId P4 = new PlayerId("p4");

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
            assertThat(trick.getTurns()).isEmpty();
            assertThat(trick.getStartingPlayerId()).isEqualTo(P1);
            assertThat(trick.getWinningPlayerId()).isNull();
            assertThat(trick.getLeadingSuit()).isNull();
            assertThat(trick.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("Constructor should throw if starting player is null")
        void constructorShouldThrowOnNullStarter() {
            assertThatThrownBy(() -> new Trick(null, Suit.HEARTS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Starting player ID must exist");
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

            assertThat(trick.getTurns()).hasSize(1);
            assertThat(trick.getLeadingSuit()).isEqualTo(Suit.HEARTS);
            assertThat(trick.getWinningPlayerId()).isEqualTo(P1);
        }

        @Test
        @DisplayName("Should throw if same player tries to play twice")
        void shouldPreventDuplicatePlayer() {
            trick.addTurn(P1, new Card(Suit.HEARTS, Rank.ACE));

            assertThatThrownBy(() -> trick.addTurn(P1, new Card(Suit.HEARTS, Rank.KING)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already played");
        }

        @Test
        @DisplayName("Should throw if adding a 5th turn to a full trick")
        void shouldPreventExtraTurns() {
            playFullTrick();

            assertThatThrownBy(() -> trick.addTurn(new PlayerId("p5"), new Card(Suit.DIAMONDS, Rank.TWO)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already full");
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

            assertThat(trick.getWinningPlayerId()).isEqualTo(P2);
        }

        @Test
        @DisplayName("Any trump card should win over the leading suit")
        void trumpShouldBeatLead() {
            // Trump is CLUBS (set in setUp)
            trick.addTurn(P1, new Card(Suit.HEARTS, Rank.ACE));
            trick.addTurn(P2, new Card(Suit.CLUBS, Rank.TWO)); // Low Trump

            assertThat(trick.getWinningPlayerId()).isEqualTo(P2);
        }

        @Test
        @DisplayName("Higher trump should win over lower trump")
        void higherTrumpShouldBeatLowerTrump() {
            trick.addTurn(P1, new Card(Suit.HEARTS, Rank.ACE));
            trick.addTurn(P2, new Card(Suit.CLUBS, Rank.TWO)); // Low Trump
            trick.addTurn(P3, new Card(Suit.CLUBS, Rank.KING)); // High Trump

            assertThat(trick.getWinningPlayerId()).isEqualTo(P3);
        }

        @Test
        @DisplayName("In a No-Trump game (Miserie), lead suit rules apply")
        void shouldHandleNoTrumpGames() {
            Trick miserieTrick = new Trick(P1, null);

            miserieTrick.addTurn(P1, new Card(Suit.HEARTS, Rank.TEN));
            miserieTrick.addTurn(P2, new Card(Suit.CLUBS, Rank.ACE)); // Would be trump, but isn't here
            miserieTrick.addTurn(P3, new Card(Suit.HEARTS, Rank.JACK)); // Beats the 10

            assertThat(miserieTrick.getWinningPlayerId()).isEqualTo(P3);
        }
    }

    private void playFullTrick() {
        trick.addTurn(P1, new Card(Suit.HEARTS, Rank.TWO));
        trick.addTurn(P2, new Card(Suit.HEARTS, Rank.THREE));
        trick.addTurn(P3, new Card(Suit.HEARTS, Rank.FOUR));
        trick.addTurn(P4, new Card(Suit.HEARTS, Rank.FIVE));
    }
}