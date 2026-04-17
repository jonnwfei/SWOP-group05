package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HighBot AI Strategy Rules")
class HighBotStrategyTest {

    private HighBotStrategy strategy;
    private PlayerId botId;

    @BeforeEach
    void setUp() {
        strategy = new HighBotStrategy();
        botId = new PlayerId();
    }

    @Nested
    @DisplayName("Bidding Phase Logic")
    class BiddingTests {

        @Test
        @DisplayName("determineBid() should always return a PASS bid")
        void alwaysReturnsPassBid() {
            List<Card> dummyHand = List.of(new Card(Suit.HEARTS, Rank.ACE));

            // Passing the ID and Hand, completely decoupled from the Player object!
            Bid bid = strategy.determineBid(botId, dummyHand);

            assertNotNull(bid);
            assertEquals(BidType.PASS, bid.getType());
            assertEquals(botId, bid.getPlayerId());
        }
    }

    @Nested
    @DisplayName("Play Phase Logic (chooseCardToPlay)")
    class PlayingTests {

        @Test
        @DisplayName("Should throw exception if hand is null or empty")
        void throwsOnInvalidHand() {
            assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(null, Suit.HEARTS));
            assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(List.of(), Suit.HEARTS));
        }

        @Test
        @DisplayName("Must follow lead suit: Plays the highest card of the requested lead suit")
        void playsHighestOfLeadSuit() {
            Card heartTwo = new Card(Suit.HEARTS, Rank.TWO);
            Card heartQueen = new Card(Suit.HEARTS, Rank.QUEEN);
            Card spadeAce = new Card(Suit.SPADES, Rank.ACE); // Higher overall, but wrong suit

            List<Card> hand = List.of(heartTwo, heartQueen, spadeAce);

            Card played = strategy.chooseCardToPlay(hand, Suit.HEARTS);

            assertEquals(heartQueen, played, "Must follow suit and pick the highest Heart, ignoring the Spade Ace.");
        }

        @Test
        @DisplayName("Void in lead suit: Plays the highest overall card in hand")
        void playsHighestOverallWhenVoid() {
            Card diamondTen = new Card(Suit.DIAMONDS, Rank.TEN);
            Card clubKing = new Card(Suit.CLUBS, Rank.KING);

            List<Card> hand = List.of(diamondTen, clubKing);

            Card played = strategy.chooseCardToPlay(hand, Suit.SPADES);

            assertEquals(clubKing, played, "Cannot follow Spades, so must play the highest card available (Club King).");
        }

        @Test
        @DisplayName("Leading the trick (Lead suit is null): Plays the highest overall card in hand")
        void playsHighestOverallWhenLeading() {
            Card heartTwo = new Card(Suit.HEARTS, Rank.TWO);
            Card spadeAce = new Card(Suit.SPADES, Rank.ACE);

            List<Card> hand = List.of(heartTwo, spadeAce);

            Card played = strategy.chooseCardToPlay(hand, null);

            assertEquals(spadeAce, played, "When leading the trick, should open with the strongest card.");
        }
    }

    @Nested
    @DisplayName("System Configuration")
    class ConfigTests {

        @Test
        @DisplayName("requiresConfirmation() is always false for bots")
        void doesNotRequireConfirmation() {
            assertFalse(strategy.requiresConfirmation(), "Bots must run automatically without UI confirmation.");
        }
    }
}