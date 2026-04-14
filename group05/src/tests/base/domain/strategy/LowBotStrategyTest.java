package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.PassBid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LowBotStrategyTest {

    private LowBotStrategy strategy;
    private Player botPlayer;

    @BeforeEach
    void setUp() {
        strategy = new LowBotStrategy();
        botPlayer = new Player(strategy, "LowBot");
    }

    @Test
    void determineBid_AlwaysReturnsPassBid() {
        Bid bid = strategy.determineBid(botPlayer);
        assertTrue(bid instanceof PassBid);
        assertEquals(BidType.PASS, bid.getType());
    }

    @Test
    void chooseCardToPlay_MustFollowLead_PlaysLowestOfLead() {
        // Hand bevat Harten 2, Harten Vrouw en Schoppen 3
        List<Card> hand = new ArrayList<>();
        Card heart2 = new Card(Suit.HEARTS, Rank.TWO);
        Card heartQueen = new Card(Suit.HEARTS, Rank.QUEEN);
        Card spade3 = new Card(Suit.SPADES, Rank.THREE);
        hand.add(heart2);
        hand.add(heartQueen);
        hand.add(spade3);

        // Er wordt Harten gevraagd (Lead)
        Card played = strategy.chooseCardToPlay(hand, Suit.HEARTS);

        // De bot moet Harten bekennen en de LAAGSTE kiezen (2),
        // ook al is Schoppen 3 lager dan de Vrouw.
        assertEquals(heart2, played, "Bot moet de laagste kaart van de gevraagde kleur spelen.");
    }

    @Test
    void chooseCardToPlay_CannotFollowLead_PlaysLowestOverall() {
        // Hand bevat alleen Klaveren Heer en Ruiten 10
        List<Card> hand = new ArrayList<>();
        Card clubKing = new Card(Suit.CLUBS, Rank.KING);
        Card diamond10 = new Card(Suit.DIAMONDS, Rank.TEN);
        hand.add(clubKing);
        hand.add(diamond10);

        // Er wordt Schoppen gevraagd (die de bot niet heeft)
        Card played = strategy.chooseCardToPlay(hand, Suit.SPADES);

        // De bot kan niet bekennen, dus speelt hij zijn absolute laagste kaart (Ruiten 10)
        assertEquals(diamond10, played, "Bot moet zijn laagste kaart spelen als hij de kleur niet heeft.");
    }

    @Test
    void chooseCardToPlay_EmptyOrNullHand_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(null, Suit.CLUBS));
        assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(new ArrayList<>(), Suit.CLUBS));
    }

    @Test
    void requiresConfirmation_IsAlwaysFalse() {
        assertFalse(strategy.requiresConfirmation());
    }
}