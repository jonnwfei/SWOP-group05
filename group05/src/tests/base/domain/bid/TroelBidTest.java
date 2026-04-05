package base.domain.bid;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TroelBidTest {

    private Player testPlayer;
    private BidType troelType;
    private BidType troelaType;

    @BeforeEach
    void setUp() {
        testPlayer = new Player(new HumanStrategy(), "TroelPlayer");
        troelType = BidType.TROEL;
        troelaType = BidType.TROELA;
    }

    /** Helper method to quickly give the testPlayer a hand with specific Aces */
    private void setAcesInHand(Suit... aceSuits) {
        List<Card> hand = new ArrayList<>();
        // Add the requested aces
        for (Suit suit : aceSuits) {
            hand.add(new Card(suit, Rank.ACE));
        }
        // Fill the rest of the hand with dummy low cards so it's a valid hand (optional but safe)
        while (hand.size() < 13) {
            hand.add(new Card(Suit.SPADES, Rank.TWO));
        }
        testPlayer.setHand(hand);
    }

    // -------- CONSTRUCTOR TESTS --------

    @Test
    void constructor_NullParameters_ThrowsException() {
        setAcesInHand(Suit.HEARTS, Suit.DIAMONDS, Suit.SPADES); // 3 Aces

        // Null player
        assertThrows(IllegalArgumentException.class, () ->
                new TroelBid(null, troelType)
        );

        // Null bidType
        assertThrows(IllegalArgumentException.class, () ->
                new TroelBid(testPlayer, null)
        );
    }

    @Test
    void constructor_InvalidCategory_ThrowsException() {
        setAcesInHand(Suit.HEARTS, Suit.DIAMONDS, Suit.SPADES);
        assertThrows(IllegalArgumentException.class, () ->
                new TroelBid(testPlayer, BidType.MISERIE)
        );
    }

    @Test
    void constructor_NotEnoughAces_ThrowsException() {
        setAcesInHand(Suit.HEARTS, Suit.SPADES); // Only 2 Aces

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new TroelBid(testPlayer, troelType)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("conditions"));
    }

    @Test
    void constructor_MismatchedAcesAndBidType_ThrowsException() {
        // Case 1: Has 4 Aces, but tries to bid regular TROEL
        setAcesInHand(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS, Suit.CLUBS);
        assertThrows(IllegalArgumentException.class, () ->
                new TroelBid(testPlayer, troelType)
        );

        // Case 2: Has 3 Aces, but tries to bid TROELA
        setAcesInHand(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS);
        assertThrows(IllegalArgumentException.class, () ->
                new TroelBid(testPlayer, troelaType)
        );
    }

    // -------- GETTER & ACCESSOR TESTS --------

    @Test
    void gettersAndAccessors_ReturnCorrectData() {
        setAcesInHand(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS); // 3 Aces -> Valid Troel
        TroelBid bid = new TroelBid(testPlayer, troelType);

        assertEquals(testPlayer, bid.getPlayer());
        assertEquals(troelType, bid.getType());
        assertEquals(testPlayer, bid.player());
        assertEquals(troelType, bid.bidType());
    }

    // -------- TRUMP SELECTION TESTS --------

    @Test
    void determineTrump_Troel_ReturnsMissingAceSuit() {
        // Player has Hearts, Spades, and Diamonds. Missing Clubs.
        setAcesInHand(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS);
        TroelBid bid = new TroelBid(testPlayer, troelType);

        // Trump is the suit of the fourth ace [cite: 191-192]
        assertEquals(Suit.CLUBS, bid.determineTrump(Suit.HEARTS));
    }

    @Test
    void determineTrump_Troela_ReturnsHearts() {
        // Player has all 4 Aces
        setAcesInHand(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS, Suit.CLUBS);
        TroelBid bid = new TroelBid(testPlayer, troelaType);

        // Trump is ALWAYS Hearts for Troela [cite: 194-195]
        assertEquals(Suit.HEARTS, bid.determineTrump(Suit.SPADES));
    }

    // -------- SCORING TESTS --------

    @Test
    void calculateBasePoints_Troel_Success() {
        setAcesInHand(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS);
        TroelBid bid = new TroelBid(testPlayer, troelType);

        int target = troelType.getTargetTricks(); // 8
        int base = troelType.getBasePoints();     // 4

        // Exact target: 4 points
        assertEquals(base, bid.calculateBasePoints(target));

        // Overtricks (+2 for each excess trick) [cite: 229]
        // 10 tricks -> 2 excess -> 4 + 4 = 8
        assertEquals(base + 4, bid.calculateBasePoints(10));
    }

    @Test
    void calculateBasePoints_Troela_Success() {
        setAcesInHand(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS, Suit.CLUBS);
        TroelBid bid = new TroelBid(testPlayer, troelaType);

        int target = troelaType.getTargetTricks(); // 9
        int base = troelaType.getBasePoints();     // 4

        // Exact target: 4 points
        assertEquals(base, bid.calculateBasePoints(target));

        // Overtricks (+2 for each excess trick)
        // 11 tricks -> 2 excess -> 4 + 4 = 8
        assertEquals(base + 4, bid.calculateBasePoints(11));
    }

    @Test
    void calculateBasePoints_Slam_DoublesPoints() {
        setAcesInHand(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS);
        TroelBid bid = new TroelBid(testPlayer, troelType);

        int target = troelType.getTargetTricks(); // 8
        int base = troelType.getBasePoints();     // 4

        // 13 tricks -> 5 excess -> 4 + 10 = 14 -> Doubled to 28 [cite: 229]
        int excess = 13 - target;
        int expected = (base + (excess * 2)) * 2;

        assertEquals(expected, bid.calculateBasePoints(13));
    }

    @Test
    void calculateBasePoints_Failure() {
        setAcesInHand(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS);
        TroelBid bid = new TroelBid(testPlayer, troelType);

        // Failed bid returns negative base points
        assertEquals(-troelType.getBasePoints(), bid.calculateBasePoints(7));
    }

    @Test
    void calculateBasePoints_OutOfBounds_ThrowsException() {
        setAcesInHand(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS);
        TroelBid bid = new TroelBid(testPlayer, troelType);

        assertThrows(IllegalArgumentException.class, () -> bid.calculateBasePoints(-1));
    }
}