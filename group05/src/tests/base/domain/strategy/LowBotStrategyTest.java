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

@DisplayName("LowBot AI Strategy Rules")
class LowBotStrategyTest {

    private LowBotStrategy strategy;
    private PlayerId botId;

    @BeforeEach
    void setUp() {
        strategy = new LowBotStrategy();
        botId = new PlayerId("low-bot-002");
    }

    @Nested
    @DisplayName("Bidding Phase Logic")
    class BiddingTests {

        @Test
        @DisplayName("determineBid() should always return a PASS bid")
        void alwaysReturnsPassBid() {
            List<Card> dummyHand = List.of(new Card(Suit.HEARTS, Rank.TWO));

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
        @DisplayName("Must follow lead suit: Plays the lowest card of the requested lead suit")
        void playsLowestOfLeadSuit() {
            Card heartTwo = new Card(Suit.HEARTS, Rank.TWO);
            Card heartQueen = new Card(Suit.HEARTS, Rank.QUEEN);
            Card spadeThree = new Card(Suit.SPADES, Rank.THREE);

            List<Card> hand = List.of(heartTwo, heartQueen, spadeThree);

            Card played = strategy.chooseCardToPlay(hand, Suit.HEARTS);

            assertEquals(heartTwo, played, "Must follow suit and pick the lowest Heart, ignoring the Spade.");
        }

        @Test
        @DisplayName("Void in lead suit: Plays the absolute lowest card in hand")
        void playsLowestOverallWhenVoid() {
            Card clubKing = new Card(Suit.CLUBS, Rank.KING);
            Card diamondTen = new Card(Suit.DIAMONDS, Rank.TEN);

            List<Card> hand = List.of(clubKing, diamondTen);

            Card played = strategy.chooseCardToPlay(hand, Suit.SPADES);

            assertEquals(diamondTen, played, "Cannot follow Spades, so must play the lowest card available (Diamond Ten).");
        }

        @Test
        @DisplayName("Leading the trick (Lead suit is null): Plays the lowest overall card in hand")
        void playsLowestOverallWhenLeading() {
            Card heartQueen = new Card(Suit.HEARTS, Rank.QUEEN);
            Card spadeThree = new Card(Suit.SPADES, Rank.THREE);

            List<Card> hand = List.of(heartQueen, spadeThree);

            Card played = strategy.chooseCardToPlay(hand, null);

            assertEquals(spadeThree, played, "When leading the trick, should open with the weakest card.");
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