package base.domain.trick;

import base.domain.card.Card;
import base.domain.card.CardMath;
import base.domain.card.Rank;
import base.domain.card.Suit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CardMath.doesCardBeat() — Trick-Winning Evaluation Rules")
class TrickEvaluatorTest {

    @Nested
    @DisplayName("Validation Constraints")
    class ValidationTests {

        @Test
        @DisplayName("Should throw NullPointerException when challenger is null")
        void shouldThrowWhenChallengerIsNull() {
            Card validCard = new Card(Suit.HEARTS, Rank.TEN);
            assertThrows(
                    NullPointerException.class,
                    () -> CardMath.doesCardBeat(null, validCard, Suit.HEARTS, Suit.SPADES)
            );
        }

        @Test
        @DisplayName("Should return true when incumbent is null (first card played always leads)")
        void shouldReturnTrueWhenIncumbentIsNull() {
            Card challenger = new Card(Suit.HEARTS, Rank.TEN);
            assertTrue(CardMath.doesCardBeat(challenger, null, Suit.HEARTS, Suit.SPADES));
        }

        @Test
        @DisplayName("Should evaluate correctly when Trump Suit is null (Miserie mode)")
        void shouldAllowNullTrumpSuit() {
            Card leadAce = new Card(Suit.CLUBS, Rank.ACE);
            Card offSuit = new Card(Suit.HEARTS, Rank.TWO);
            // In no-trump, lead-suit Ace beats an off-suit Two
            assertFalse(CardMath.doesCardBeat(offSuit, leadAce, Suit.CLUBS, null));
        }
    }

    @Nested
    @DisplayName("Trump Suit Contests")
    class TrumpTests {

        @Test
        @DisplayName("Trump card should always beat a non-trump card")
        void trumpBeatsNonTrump() {
            Card currentBest = new Card(Suit.SPADES, Rank.ACE);   // Lead suit, highest rank
            Card challenger  = new Card(Suit.HEARTS, Rank.TWO);   // Trump suit, lowest rank

            // Lead is SPADES, Trump is HEARTS
            assertTrue(CardMath.doesCardBeat(challenger, currentBest, Suit.SPADES, Suit.HEARTS));
        }

        @Test
        @DisplayName("Non-trump card should never beat a trump card")
        void nonTrumpLosesToTrump() {
            Card currentBest = new Card(Suit.HEARTS, Rank.TWO);   // Trump suit
            Card challenger  = new Card(Suit.SPADES, Rank.ACE);   // Lead suit

            assertFalse(CardMath.doesCardBeat(challenger, currentBest, Suit.SPADES, Suit.HEARTS));
        }

        @ParameterizedTest(name = "Challenger {0} vs Current Best {1} -> expects {2}")
        @CsvSource({
                "ACE,   KING,  true",   // Higher trump beats lower trump
                "TEN,   JACK,  false",  // Lower trump loses to higher trump
                "EIGHT, EIGHT, false"   // Identical rank does not "beat" (must be strictly greater)
        })
        @DisplayName("When both are Trumps, the higher rank wins")
        void higherTrumpWins(Rank challengerRank, Rank currentBestRank, boolean expectedResult) {
            Card challenger  = new Card(Suit.HEARTS, challengerRank);
            Card currentBest = new Card(Suit.HEARTS, currentBestRank);

            // Lead is SPADES, Trump is HEARTS
            assertEquals(expectedResult, CardMath.doesCardBeat(challenger, currentBest, Suit.SPADES, Suit.HEARTS));
        }
    }

    @Nested
    @DisplayName("Lead Suit Contests (No Trump involved)")
    class LeadSuitTests {

        @ParameterizedTest(name = "Challenger {0} vs Current Best {1} -> expects {2}")
        @CsvSource({
                "QUEEN, JACK, true",   // Higher lead beats lower lead
                "NINE,  TEN,  false"   // Lower lead loses to higher lead
        })
        @DisplayName("When both are Lead suit, the higher rank wins")
        void higherLeadWins(Rank challengerRank, Rank currentBestRank, boolean expectedResult) {
            Card challenger  = new Card(Suit.CLUBS, challengerRank);
            Card currentBest = new Card(Suit.CLUBS, currentBestRank);

            // Lead is CLUBS, Trump is HEARTS
            assertEquals(expectedResult, CardMath.doesCardBeat(challenger, currentBest, Suit.CLUBS, Suit.HEARTS));
        }

        @Test
        @DisplayName("Off-suit card (not lead, not trump) always loses to Lead suit")
        void offSuitLosesToLead() {
            Card currentBest = new Card(Suit.CLUBS, Rank.TWO);      // Lead suit
            Card challenger  = new Card(Suit.DIAMONDS, Rank.ACE);   // Off-suit, high rank

            assertFalse(CardMath.doesCardBeat(challenger, currentBest, Suit.CLUBS, Suit.HEARTS));
        }

        @Test
        @DisplayName("Off-suit card always loses to another off-suit card (first to play wins ties)")
        void offSuitLosesToOffSuit() {
            Card currentBest = new Card(Suit.DIAMONDS, Rank.TEN);   // Off-suit
            Card challenger  = new Card(Suit.SPADES, Rank.ACE);     // Different off-suit, higher rank

            // Lead is CLUBS, Trump is HEARTS. Neither DIAMONDS nor SPADES is lead/trump.
            assertFalse(CardMath.doesCardBeat(challenger, currentBest, Suit.CLUBS, Suit.HEARTS));
        }

        @Test
        @DisplayName("Off-suit challenger (non-lead, non-trump) loses to a trump card")
        void offSuitLosesToTrump() {
            Card currentBest = new Card(Suit.HEARTS, Rank.TWO);     // Trump suit
            Card challenger  = new Card(Suit.DIAMONDS, Rank.ACE);   // Off-suit (non-lead, non-trump)

            // Lead is SPADES, Trump is HEARTS
            assertFalse(CardMath.doesCardBeat(challenger, currentBest, Suit.SPADES, Suit.HEARTS),
                    "An off-suit card should never beat a trump card.");
        }
    }

    @Nested
    @DisplayName("Miserie / Null Trump Mechanics")
    class MiserieTests {

        @Test
        @DisplayName("Should evaluate purely on Lead suit when Trump is null")
        void operatesWithoutTrumpSuit() {
            Card leadLow    = new Card(Suit.DIAMONDS, Rank.TWO);
            Card leadHigh   = new Card(Suit.DIAMONDS, Rank.KING);
            Card offSuitAce = new Card(Suit.SPADES, Rank.ACE);

            // Lead is DIAMONDS, Trump is NULL (Miserie)
            assertTrue(CardMath.doesCardBeat(leadHigh, leadLow, Suit.DIAMONDS, null));
            assertFalse(CardMath.doesCardBeat(offSuitAce, leadHigh, Suit.DIAMONDS, null));
        }
    }
}
