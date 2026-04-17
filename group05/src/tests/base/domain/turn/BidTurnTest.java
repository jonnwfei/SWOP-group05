package base.domain.turn;

import base.domain.bid.BidType;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("BidTurn Record Tests")
class BidTurnTest {

    @Test
    @DisplayName("Successfully creates BidTurn with valid arguments")
    void testValidBidTurnCreation() {
        PlayerId mockPlayerId = mock(PlayerId.class);
        BidType mockBidType = BidType.PROPOSAL; // Assuming BidType is an enum, otherwise mock it

        BidTurn bidTurn = new BidTurn(mockPlayerId, mockBidType);

        assertEquals(mockPlayerId, bidTurn.playerId(), "PlayerId should match the provided instance.");
        assertEquals(mockBidType, bidTurn.bidType(), "BidType should match the provided instance.");
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when PlayerId is null")
    void testNullPlayerIdThrowsException() {
        BidType mockBidType = BidType.PROPOSAL;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new BidTurn(null, mockBidType)
        );

        assertEquals("Bid turn: Player cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when BidType is null")
    void testNullBidTypeThrowsException() {
        PlayerId mockPlayerId = mock(PlayerId.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new BidTurn(mockPlayerId, null)
        );

        assertEquals("Bid turn: bidType cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Validates auto-generated equality for the record")
    void testRecordEquality() {
        PlayerId mockPlayerId = mock(PlayerId.class);
        BidType mockBidType = BidType.PASS;

        BidTurn turn1 = new BidTurn(mockPlayerId, mockBidType);
        BidTurn turn2 = new BidTurn(mockPlayerId, mockBidType);

        assertEquals(turn1, turn2, "Two records with identical data should be equal");
    }
}