package base.domain.trick;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TrickEvaluator Rules & Mechanics")
class TrickEvaluatorTest {

    @Nested
    @DisplayName("Constructor & Validation Constraints")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when Lead Suit is null")
        void shouldThrowWhenLeadSuitIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new TrickEvaluator(null, Suit.HEARTS)
            );
            assertTrue(exception.getMessage().contains("leadSuit must not be null"));
        }

        @Test
        @DisplayName("Should create successfully when Trump Suit is null (Miserie)")
        void shouldAllowNullTrumpSuit() {
            TrickEvaluator evaluator = new TrickEvaluator(Suit.CLUBS, null);
            assertNotNull(evaluator, "Evaluator should be instantiated even without a trump suit.");
        }

        @Test
        @DisplayName("Should throw exception when cards are null in doesBeat()")
        void shouldThrowWhenCardsAreNull() {
            TrickEvaluator evaluator = new TrickEvaluator(Suit.HEARTS, Suit.SPADES);
            Card validCard = new Card(Suit.HEARTS, Rank.TEN);

            assertThrows(IllegalArgumentException.class, () -> evaluator.doesBeat(null, validCard));
            assertThrows(IllegalArgumentException.class, () -> evaluator.doesBeat(validCard, null));
        }
    }

    @Nested
    @DisplayName("Trump Suit Contests")
    class TrumpTests {
        private TrickEvaluator evaluator;

        @BeforeEach
        void setUp() {
            // Lead is SPADES, Trump is HEARTS
            evaluator = new TrickEvaluator(Suit.SPADES, Suit.HEARTS);
        }

        @Test
        @DisplayName("Trump card should always beat a non-trump card")
        void trumpBeatsNonTrump() {
            Card currentBest = new Card(Suit.SPADES, Rank.ACE);   // Lead suit, highest rank
            Card challenger = new Card(Suit.HEARTS, Rank.TWO);    // Trump suit, lowest rank

            assertTrue(evaluator.doesBeat(challenger, currentBest));
        }

        @Test
        @DisplayName("Non-trump card should never beat a trump card")
        void nonTrumpLosesToTrump() {
            Card currentBest = new Card(Suit.HEARTS, Rank.TWO);   // Trump suit
            Card challenger = new Card(Suit.SPADES, Rank.ACE);    // Lead suit

            assertFalse(evaluator.doesBeat(challenger, currentBest));
        }

        @ParameterizedTest(name = "Challenger {0} vs Current Best {1} -> expects {2}")
        @CsvSource({
                "ACE, KING, true",   // Higher trump beats lower trump
                "TEN, JACK, false",  // Lower trump loses to higher trump
                "EIGHT, EIGHT, false" // Identical rank does not "beat" (must be strictly greater)
        })
        @DisplayName("When both are Trumps, the higher rank wins")
        void higherTrumpWins(Rank challengerRank, Rank currentBestRank, boolean expectedResult) {
            Card challenger = new Card(Suit.HEARTS, challengerRank);
            Card currentBest = new Card(Suit.HEARTS, currentBestRank);

            assertEquals(expectedResult, evaluator.doesBeat(challenger, currentBest));
        }
    }

    @Nested
    @DisplayName("Lead Suit Contests (No Trump involved)")
    class LeadSuitTests {
        private TrickEvaluator evaluator;

        @BeforeEach
        void setUp() {
            // Lead is CLUBS, Trump is HEARTS
            evaluator = new TrickEvaluator(Suit.CLUBS, Suit.HEARTS);
        }

        @ParameterizedTest(name = "Challenger {0} vs Current Best {1} -> expects {2}")
        @CsvSource({
                "QUEEN, JACK, true",   // Higher lead beats lower lead
                "NINE, TEN, false",    // Lower lead loses to higher lead
        })
        @DisplayName("When both are Lead suit, the higher rank wins")
        void higherLeadWins(Rank challengerRank, Rank currentBestRank, boolean expectedResult) {
            Card challenger = new Card(Suit.CLUBS, challengerRank);
            Card currentBest = new Card(Suit.CLUBS, currentBestRank);

            assertEquals(expectedResult, evaluator.doesBeat(challenger, currentBest));
        }

        @Test
        @DisplayName("Off-suit card (not lead, not trump) always loses to Lead suit")
        void offSuitLosesToLead() {
            Card currentBest = new Card(Suit.CLUBS, Rank.TWO);      // Lead suit
            Card challenger = new Card(Suit.DIAMONDS, Rank.ACE);    // Off-suit, high rank

            assertFalse(evaluator.doesBeat(challenger, currentBest));
        }

        @Test
        @DisplayName("Off-suit card always loses to another off-suit card (first to play wins ties)")
        void offSuitLosesToOffSuit() {
            Card currentBest = new Card(Suit.DIAMONDS, Rank.TEN);   // Off-suit
            Card challenger = new Card(Suit.SPADES, Rank.ACE);      // Off-suit, higher rank

            // Neither is lead, neither is trump. Challenger cannot "beat" the current best.
            assertFalse(evaluator.doesBeat(challenger, currentBest));
        }
    }

    @Nested
    @DisplayName("Miserie / Null Trump Mechanics")
    class MiserieTests {

        @Test
        @DisplayName("Should evaluate purely on Lead suit when Trump is null")
        void operatesWithoutTrumpSuit() {
            TrickEvaluator miserieEvaluator = new TrickEvaluator(Suit.DIAMONDS, null);

            Card leadLow = new Card(Suit.DIAMONDS, Rank.TWO);
            Card leadHigh = new Card(Suit.DIAMONDS, Rank.KING);
            Card offSuitAce = new Card(Suit.SPADES, Rank.ACE);

            assertTrue(miserieEvaluator.doesBeat(leadHigh, leadLow));

            assertFalse(miserieEvaluator.doesBeat(offSuitAce, leadHigh));
        }
    }
}