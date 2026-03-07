package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MiserieBidTest {

    private Player testPlayer;
    private BidType miserieNormal;

    @BeforeEach
    void setUp() {
        testPlayer = new Player(new HumanStrategy(), "Jane Doe");
        // TODO: Controleer of 'MISERIE' de juiste naam is in jouw BidType Enum
        miserieNormal = BidType.MISERIE;
    }

    @Test
    void constructor_InvalidCategory_ThrowsException() {
        // Test de GRASP invariant: mag geen ABONDANCE aanmaken in een MiserieBid
        assertThrows(IllegalArgumentException.class, () ->
                new MiserieBid(testPlayer, BidType.ABONDANCE_9)
        );
    }

    @Test
    void getChosenTrump_AlwaysReturnsNull() {
        MiserieBid bid = new MiserieBid(testPlayer, miserieNormal);
        // Bij miserie wordt er altijd 'sans atout' (zonder troef) gespeeld
        assertNull(bid.getChosenTrump(Suit.SPADES));
        assertNull(bid.getChosenTrump(null));
    }

    @Test
    void calculateBasePoints_Success_ZeroTricks() {
        MiserieBid bid = new MiserieBid(testPlayer, miserieNormal);
        int expectedPoints = miserieNormal.getBasePoints();

        // Scenario: 0 slagen gehaald (Perfecte miserie)
        assertEquals(expectedPoints, bid.calculateBasePoints(0));
    }

    @Test
    void calculateBasePoints_Failure_OneOrMoreTricks() {
        MiserieBid bid = new MiserieBid(testPlayer, miserieNormal);
        int negativePoints = -1 * miserieNormal.getBasePoints();

        // Scenario: 1 slag gehaald (Miserie kapot)
        // TODO: In sommige Whist-varianten verlies je meer per extra slag,
        // maar jouw code geeft nu een vaste negatieve waarde.
        assertEquals(negativePoints, bid.calculateBasePoints(1));
        assertEquals(negativePoints, bid.calculateBasePoints(13));
    }

    @Test
    void calculateBasePoints_NegativeInput_ThrowsException() {
        MiserieBid bid = new MiserieBid(testPlayer, miserieNormal);
        assertThrows(IllegalArgumentException.class, () -> bid.calculateBasePoints(-1));
    }

    @Test
    void getPlayerAndType() {
        MiserieBid bid = new MiserieBid(testPlayer, miserieNormal);
        assertEquals(testPlayer, bid.getPlayer());
        assertEquals(miserieNormal, bid.getType());
    }
}