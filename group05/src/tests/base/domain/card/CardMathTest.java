package base.domain.card;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CardMath Utility Rules")
class CardMathTest {

    @Nested
    @DisplayName("getLegalCards()")
    class GetLegalCardsTests {

        @Test
        @DisplayName("Should return the entire hand when lead suit is null")
        void returnsHandWhenLeadIsNull() {
            List<Card> hand = List.of(
                    new Card(Suit.HEARTS, Rank.TEN),
                    new Card(Suit.SPADES, Rank.ACE)
            );

            List<Card> legalCards = CardMath.getLegalCards(hand, null);

            assertEquals(2, legalCards.size());
            assertTrue(legalCards.containsAll(hand));
        }

        @Test
        @DisplayName("Should return only cards matching the lead suit if player has them")
        void returnsOnlyLeadSuitWhenPresent() {
            Card heartTen = new Card(Suit.HEARTS, Rank.TEN);
            Card heartAce = new Card(Suit.HEARTS, Rank.ACE);
            Card spadeKing = new Card(Suit.SPADES, Rank.KING);
            List<Card> hand = List.of(heartTen, heartAce, spadeKing);

            List<Card> legalCards = CardMath.getLegalCards(hand, Suit.HEARTS);

            assertEquals(2, legalCards.size());
            assertTrue(legalCards.contains(heartTen));
            assertTrue(legalCards.contains(heartAce));
            assertFalse(legalCards.contains(spadeKing));
        }

        @Test
        @DisplayName("Should return the entire hand if player is void in the lead suit")
        void returnsEntireHandWhenVoidInLeadSuit() {
            List<Card> hand = List.of(
                    new Card(Suit.HEARTS, Rank.TEN),
                    new Card(Suit.SPADES, Rank.ACE)
            );

            List<Card> legalCards = CardMath.getLegalCards(hand, Suit.CLUBS);

            assertEquals(2, legalCards.size());
            assertTrue(legalCards.containsAll(hand));
        }
    }

    @Nested
    @DisplayName("findHighestCards() & findLowestCards()")
    class ExtremesTests {

        @Test
        @DisplayName("Should return an empty list when input is null or empty")
        void handlesNullOrEmpty() {
            assertTrue(CardMath.findHighestCards(null).isEmpty());
            assertTrue(CardMath.findHighestCards(List.of()).isEmpty());

            assertTrue(CardMath.findLowestCards(null).isEmpty());
            assertTrue(CardMath.findLowestCards(List.of()).isEmpty());
        }

        @Test
        @DisplayName("Highest: Should return all cards tied for the highest rank")
        void returnsMultipleHighestCardsIfTied() {
            Card highHeart = new Card(Suit.HEARTS, Rank.ACE);
            Card highSpade = new Card(Suit.SPADES, Rank.ACE);
            Card lowClub = new Card(Suit.CLUBS, Rank.TWO);

            List<Card> result = CardMath.findHighestCards(List.of(highHeart, highSpade, lowClub));

            assertEquals(2, result.size());
            assertTrue(result.contains(highHeart) && result.contains(highSpade));
        }

        @Test
        @DisplayName("Lowest: Should return all cards tied for the lowest rank")
        void returnsMultipleLowestCardsIfTied() {
            Card lowHeart = new Card(Suit.HEARTS, Rank.TWO);
            Card lowSpade = new Card(Suit.SPADES, Rank.TWO);
            Card highClub = new Card(Suit.CLUBS, Rank.ACE);

            List<Card> result = CardMath.findLowestCards(List.of(lowHeart, lowSpade, highClub));

            assertEquals(2, result.size());
            assertTrue(result.contains(lowHeart) && result.contains(lowSpade));
        }
    }

    @Nested
    @DisplayName("doesCardBeat()")
    class DoesCardBeatTests {

        @Test
        @DisplayName("Should return true automatically if there is no current winning card")
        void beatsNullCurrentBest() {
            Card challenger = new Card(Suit.HEARTS, Rank.TWO);
            assertTrue(CardMath.doesCardBeat(challenger, null, Suit.HEARTS, Suit.SPADES));
        }

        @ParameterizedTest(name = "Trump vs Lead/Off-Suit -> Trump wins")
        @CsvSource({
                "HEARTS, TWO, SPADES, ACE, true",  // Trump challenger beats Lead currentBest
                "SPADES, ACE, HEARTS, TWO, false"  // Lead challenger loses to Trump currentBest
        })
        @DisplayName("Trump suit interactions against non-trump suits")
        void trumpVsNonTrump(Suit challengerSuit, Rank challengerRank, Suit bestSuit, Rank bestRank, boolean expected) {
            Card challenger = new Card(challengerSuit, challengerRank);
            Card currentBest = new Card(bestSuit, bestRank);

            // Lead is SPADES, Trump is HEARTS
            assertEquals(expected, CardMath.doesCardBeat(challenger, currentBest, Suit.SPADES, Suit.HEARTS));
        }

        @ParameterizedTest(name = "Challenger {0} vs Best {1} -> Challenger wins: {2}")
        @CsvSource({
                "ACE, KING, true",   // Higher rank beats lower rank
                "TEN, JACK, false",  // Lower rank loses to higher rank
                "EIGHT, EIGHT, false" // Equal rank loses (must strictly beat)
        })
        @DisplayName("Comparing two cards of the same matching suit (Trump vs Trump, or Lead vs Lead)")
        void matchingSuitContests(Rank challengerRank, Rank bestRank, boolean expected) {
            Card challenger = new Card(Suit.HEARTS, challengerRank);
            Card currentBest = new Card(Suit.HEARTS, bestRank);

            // Works the same whether they are both trumps or both leads!
            assertEquals(expected, CardMath.doesCardBeat(challenger, currentBest, Suit.HEARTS, Suit.HEARTS));
        }

        @Test
        @DisplayName("Off-suit card (not lead, not trump) should never beat the current best")
        void offSuitLoses() {
            Card currentBest = new Card(Suit.CLUBS, Rank.TWO);      // Lead suit
            Card challenger = new Card(Suit.DIAMONDS, Rank.ACE);    // Off-suit, high rank

            // Lead is CLUBS, Trump is HEARTS
            assertFalse(CardMath.doesCardBeat(challenger, currentBest, Suit.CLUBS, Suit.HEARTS));
        }
    }

    @Nested
    @DisplayName("getHighestRankOfSuit()")
    class GetHighestRankOfSuitTests {

        @Test
        @DisplayName("Should throw exception if parameters are null")
        void throwsOnNulls() {
            assertThrows(IllegalArgumentException.class, () -> CardMath.getHighestRankOfSuit(null, List.of()));
            assertThrows(IllegalArgumentException.class, () -> CardMath.getHighestRankOfSuit(Suit.HEARTS, null));
        }

        @Test
        @DisplayName("Should return the highest rank of the requested suit")
        void returnsHighestRankWhenSuitPresent() {
            List<Card> hand = List.of(
                    new Card(Suit.HEARTS, Rank.TEN),
                    new Card(Suit.HEARTS, Rank.ACE),
                    new Card(Suit.SPADES, Rank.KING)
            );

            Rank highestHeart = CardMath.getHighestRankOfSuit(Suit.HEARTS, hand);

            assertEquals(Rank.ACE, highestHeart);
        }

        @Test
        @DisplayName("Should return null if the player is void in the requested suit")
        void returnsNullWhenVoidInSuit() {
            List<Card> hand = List.of(
                    new Card(Suit.HEARTS, Rank.TEN),
                    new Card(Suit.SPADES, Rank.KING)
            );

            Rank highestClub = CardMath.getHighestRankOfSuit(Suit.CLUBS, hand);

            assertNull(highestClub, "Should return null because hand has no Clubs.");
        }
    }
}