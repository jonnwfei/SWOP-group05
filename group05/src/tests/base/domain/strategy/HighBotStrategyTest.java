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

class HighBotStrategyTest {

    private HighBotStrategy strategy;
    private Player botPlayer;

    @BeforeEach
    void setUp() {
        strategy = new HighBotStrategy();
        botPlayer = new Player(strategy, "HighBot");
    }

    @Test
    void determineBid_AlwaysReturnsPassBid() {
        Bid bid = strategy.determineBid(botPlayer);

        assertTrue(bid instanceof PassBid);
        assertEquals(BidType.PASS, bid.getType());
        assertEquals(botPlayer, bid.getPlayer());
    }

    @Test
    void chooseCardToPlay_MustFollowLead_PlaysHighestOfLead() {
        // Hand bevat Harten 2, Harten Vrouw en Schoppen Aas
        List<Card> hand = new ArrayList<>();
        Card heart2 = new Card(Suit.HEARTS, Rank.TWO);
        Card heartQueen = new Card(Suit.HEARTS, Rank.QUEEN);
        Card spadeAce = new Card(Suit.SPADES, Rank.ACE);
        hand.add(heart2);
        hand.add(heartQueen);
        hand.add(spadeAce);

        // Er wordt Harten gevraagd (Lead)
        Card played = strategy.chooseCardToPlay(hand, Suit.HEARTS);

        // De bot moet Harten bekennen en de hoogste kiezen (Vrouw),
        // ook al is Schoppen Aas de hoogste kaart in de hele hand.
        assertEquals(heartQueen, played, "Bot moet de hoogste kaart van de gevraagde kleur spelen.");
    }

    @Test
    void chooseCardToPlay_CannotFollowLead_PlaysHighestOverall() {
        // Hand bevat alleen Ruiten en Klaveren
        List<Card> hand = new ArrayList<>();
        Card diamond10 = new Card(Suit.DIAMONDS, Rank.TEN);
        Card clubKing = new Card(Suit.CLUBS, Rank.KING);
        hand.add(diamond10);
        hand.add(clubKing);

        // Er wordt Schoppen gevraagd (die de bot niet heeft)
        Card played = strategy.chooseCardToPlay(hand, Suit.SPADES);

        // De bot kan niet bekennen, dus speelt hij zijn hoogste kaart (Klaveren Heer)
        assertEquals(clubKing, played, "Bot moet zijn hoogste kaart spelen als hij de kleur niet heeft.");
    }

    @Test
    void chooseCardToPlay_InvalidHand_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(null, Suit.HEARTS));
        assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(new ArrayList<>(), Suit.HEARTS));
    }

    @Test
    void requiresConfirmation_IsAlwaysFalse() {
        // Bots hebben geen bevestiging nodig (gebruikt voor UI/Terminal flow)
        assertFalse(strategy.requiresConfirmation());
    }
}