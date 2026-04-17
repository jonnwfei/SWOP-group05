package base.domain.turn;

import base.domain.card.Card;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("PlayTurn Record Tests")
class PlayTurnTest {

    @Test
    @DisplayName("Successfully creates PlayTurn with valid arguments")
    void testValidPlayTurnCreation() {
        PlayerId mockPlayerId = mock(PlayerId.class);
        Card mockCard = mock(Card.class);

        PlayTurn playTurn = new PlayTurn(mockPlayerId, mockCard);

        assertEquals(mockPlayerId, playTurn.playerId(), "PlayerId should match the provided instance.");
        assertEquals(mockCard, playTurn.playedCard(), "Card should match the provided instance.");
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when PlayerId is null")
    void testNullPlayerIdThrowsException() {
        Card mockCard = mock(Card.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new PlayTurn(null, mockCard)
        );

        assertEquals("Play turn: Player cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when Card is null")
    void testNullCardThrowsException() {
        PlayerId mockPlayerId = mock(PlayerId.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new PlayTurn(mockPlayerId, null)
        );

        assertEquals("Play turn: PlayedCard cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Validates auto-generated equality for the record")
    void testRecordEquality() {
        PlayerId mockPlayerId = mock(PlayerId.class);
        Card mockCard = mock(Card.class);

        PlayTurn turn1 = new PlayTurn(mockPlayerId, mockCard);
        PlayTurn turn2 = new PlayTurn(mockPlayerId, mockCard);

        assertEquals(turn1, turn2, "Two records with identical data should be equal");
    }
}