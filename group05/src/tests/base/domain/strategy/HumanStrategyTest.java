package base.domain.strategy;

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

@DisplayName("Human Strategy Rules & Stubs")
class HumanStrategyTest {

    private HumanStrategy strategy;
    private List<Card> dummyHand;

    @BeforeEach
    void setUp() {
        strategy = new HumanStrategy();
        dummyHand = List.of(new Card(Suit.HEARTS, Rank.ACE));
    }

    @Nested
    @DisplayName("Bidding Phase Logic")
    class BiddingTests {

        @Test
        @DisplayName("determineBid() should return null as UI handles human input")
        void determineBidReturnsNull() {
            // Passing the ID and Hand, completely decoupled from the Player object!
            assertNull(strategy.determineBid(dummyHand),
                    "Human strategy should return null. The UI/State machine handles actual bid creation.");
        }
    }

    @Nested
    @DisplayName("Play Phase Logic (chooseCardToPlay)")
    class PlayingTests {

        @Test
        @DisplayName("chooseCardToPlay() should return null as UI handles human input")
        void chooseCardToPlayReturnsNull() {
            assertNull(strategy.chooseCardToPlay(dummyHand, Suit.HEARTS, TeamRole.DEFENDING_TEAM),
                    "Human strategy should return null. The UI/State machine handles actual card selection.");
        }
    }
}