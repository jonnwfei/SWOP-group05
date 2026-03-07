package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbondanceBidTest {

    private Player testPlayer;
    private BidType abondance9;
    private Suit testTrump;

    @BeforeEach
    void setUp() {
        testPlayer = new Player(new HumanStrategy(), "Jane Doe");
        // TODO: Controleer of 'ABONDANCE_9' de juiste naam is in jouw BidType Enum
        abondance9 = BidType.ABONDANCE_9;
        testTrump = Suit.SPADES;
    }

    @Test
    void constructor_InvalidCategory_ThrowsException() {
        // Test de GRASP invariant: mag geen MISERIE aanmaken in een AbondanceBid
        assertThrows(IllegalArgumentException.class, () ->
                new AbondanceBid(testPlayer, BidType.MISERIE, testTrump)
        );
    }

    @Test
    void getPlayer() {
        AbondanceBid bid = new AbondanceBid(testPlayer, abondance9, testTrump);
        assertEquals(testPlayer, bid.getPlayer());
    }

    @Test
    void getType() {
        AbondanceBid bid = new AbondanceBid(testPlayer, abondance9, testTrump);
        assertEquals(abondance9, bid.getType());
    }

    @Test
    void getChosenTrump() {
        AbondanceBid bid = new AbondanceBid(testPlayer, abondance9, testTrump);
        // Bij Abondance maakt de 'dealtTrump' niet uit, de gekozen troef wint altijd
        assertEquals(testTrump, bid.getChosenTrump(Suit.HEARTS));
    }

    @Test
    void calculateBasePoints_Success() {
        AbondanceBid bid = new AbondanceBid(testPlayer, abondance9, testTrump);

        // Scenario: Gewonnen (tricksWon >= targetTricks)
        // TODO: Vul hier de verwachte punten in op basis van je BidType configuratie
        int expectedPoints = abondance9.getBasePoints();
        assertEquals(expectedPoints, bid.calculateBasePoints(9));
        assertEquals(expectedPoints, bid.calculateBasePoints(13));
    }

    @Test
    void calculateBasePoints_Failure() {
        AbondanceBid bid = new AbondanceBid(testPlayer, abondance9, testTrump);

        // Scenario: Verloren (tricksWon < targetTricks)
        int negativePoints = -1 * abondance9.getBasePoints();
        assertEquals(negativePoints, bid.calculateBasePoints(8));
        assertEquals(negativePoints, bid.calculateBasePoints(0));
    }

    @Test
    void calculateBasePoints_NegativeInput_ThrowsException() {
        AbondanceBid bid = new AbondanceBid(testPlayer, abondance9, testTrump);
        assertThrows(IllegalArgumentException.class, () -> bid.calculateBasePoints(-1));
    }

    @Test
    void testRecordAccessors() {
        AbondanceBid bid = new AbondanceBid(testPlayer, abondance9, testTrump);
        // Test de automatische record getters
        assertEquals(testPlayer, bid.player());
        assertEquals(abondance9, bid.bidType());
        assertEquals(testTrump, bid.trump());
    }
}