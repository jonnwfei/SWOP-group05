package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoloBidTest {

    private Player testPlayer;
    private BidType soloNormal;
    private Suit chosenTrump;

    @BeforeEach
    void setUp() {
        testPlayer = new Player(new HumanStrategy(), "SoloPlayer");
        // Gaat ervan uit dat BidType.SOLO bestaat en onder de categorie SOLO valt
        soloNormal = BidType.SOLO;
        chosenTrump = Suit.DIAMONDS;
    }

    @Test
    void constructor_InvalidCategory_ThrowsException() {
        // Controleert de GRASP invariant uit de compact constructor
        assertThrows(IllegalArgumentException.class, () ->
                new SoloBid(testPlayer, BidType.MISERIE, chosenTrump)
        );
    }

    @Test
    void getPlayer_ReturnsPlayer() {
        SoloBid bid = new SoloBid(testPlayer, soloNormal, chosenTrump);
        assertEquals(testPlayer, bid.getPlayer());
    }

    @Test
    void getType_ReturnsBidType() {
        SoloBid bid = new SoloBid(testPlayer, soloNormal, chosenTrump);
        assertEquals(soloNormal, bid.getType());
    }

    @Test
    void getChosenTrump_ReturnsSetTrump() {
        SoloBid bid = new SoloBid(testPlayer, soloNormal, chosenTrump);
        // De 'dealtTrump' (bijv. SPADES) wordt genegeerd ten gunste van de gekozen troef (DIAMONDS)
        assertEquals(chosenTrump, bid.getChosenTrump(Suit.SPADES));
    }

    @Test
    void calculateBasePoints_Success() {
        SoloBid bid = new SoloBid(testPlayer, soloNormal, chosenTrump);
        int base = soloNormal.getBasePoints();
        int target = soloNormal.getTargetTricks();

        // Scenario: Exact behaald
        assertEquals(base, bid.calculateBasePoints(target));
        // Scenario: Meer dan behaald (overtricks hebben geen extra waarde in basis Solo wiskunde hier)
        assertEquals(base, bid.calculateBasePoints(target + 1));
    }

    @Test
    void calculateBasePoints_Failure() {
        SoloBid bid = new SoloBid(testPlayer, soloNormal, chosenTrump);
        int base = soloNormal.getBasePoints();
        int target = soloNormal.getTargetTricks();

        // Scenario: Minder dan doelstelling resulteert in negatieve punten
        assertEquals(-base, bid.calculateBasePoints(target - 1));
    }

    @Test
    void calculateBasePoints_NegativeInput_ThrowsException() {
        SoloBid bid = new SoloBid(testPlayer, soloNormal, chosenTrump);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.calculateBasePoints(-1)
        );
        assertTrue(exception.getMessage().contains("negative tricks"));
    }
}