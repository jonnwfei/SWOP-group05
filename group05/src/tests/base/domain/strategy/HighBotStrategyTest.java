package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.TeamRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HighBot AI Strategy Rules")
class HighBotStrategyTest {

    private HighBotStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new HighBotStrategy();
    }

    @Nested
    @DisplayName("Bidding Phase Logic")
    class BiddingTests {

        @Test
        @DisplayName("determineBid() should always return a PASS bid")
        void alwaysReturnsPassBid() {
            List<Card> dummyHand = List.of(new Card(Suit.HEARTS, Rank.ACE));

            // Passing the ID and Hand, completely decoupled from the Player object!
            Bid bid = strategy.determineBid(dummyHand);

            assertNotNull(bid);
            assertEquals(BidType.PASS, bid.getType());
        }
    }

    @Nested
    @DisplayName("Play Phase Logic (chooseCardToPlay)")
    class PlayingTests {

        @Test
        @DisplayName("Should throw exception if hand is null or empty")
        void throwsOnInvalidHand() {
            assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(null, Suit.HEARTS, TeamRole.DEFENDING_TEAM));
            assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(List.of(), Suit.HEARTS, TeamRole.DEFENDING_TEAM));
        }

        @Test
        @DisplayName("Must follow lead suit: Plays the highest card of the requested lead suit")
        void playsHighestOfLeadSuit() {
            Card heartTwo = new Card(Suit.HEARTS, Rank.TWO);
            Card heartQueen = new Card(Suit.HEARTS, Rank.QUEEN);
            Card spadeAce = new Card(Suit.SPADES, Rank.ACE); // Higher overall, but wrong suit

            List<Card> hand = List.of(heartTwo, heartQueen, spadeAce);

            Card played = strategy.chooseCardToPlay(hand, Suit.HEARTS, TeamRole.DEFENDING_TEAM);

            assertEquals(heartQueen, played, "Must follow suit and pick the highest Heart, ignoring the Spade Ace.");
        }

        @Test
        @DisplayName("Void in lead suit: Plays the highest overall card in hand")
        void playsHighestOverallWhenVoid() {
            Card diamondTen = new Card(Suit.DIAMONDS, Rank.TEN);
            Card clubKing = new Card(Suit.CLUBS, Rank.KING);

            List<Card> hand = List.of(diamondTen, clubKing);

            Card played = strategy.chooseCardToPlay(hand, Suit.SPADES, TeamRole.DEFENDING_TEAM);

            assertEquals(clubKing, played, "Cannot follow Spades, so must play the highest card available (Club King).");
        }

        @Test
        @DisplayName("Leading the trick (Lead suit is null): Plays the highest overall card in hand")
        void playsHighestOverallWhenLeading() {
            Card heartTwo = new Card(Suit.HEARTS, Rank.TWO);
            Card spadeAce = new Card(Suit.SPADES, Rank.ACE);

            List<Card> hand = List.of(heartTwo, spadeAce);

            Card played = strategy.chooseCardToPlay(hand, null, TeamRole.DEFENDING_TEAM);

            assertEquals(spadeAce, played, "When leading the trick, should open with the strongest card.");
        }
    }
}