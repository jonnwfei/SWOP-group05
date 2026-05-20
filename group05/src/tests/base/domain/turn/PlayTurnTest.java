package base.domain.turn;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayTurn Record Tests")
class PlayTurnTest {

    @Test
    @DisplayName("Successfully creates PlayTurn with valid arguments")
    void testValidPlayTurnCreation() {
        PlayerId playerId = new PlayerId();
        Card card = new Card(Suit.HEARTS, Rank.ACE);

        PlayTurn playTurn = new PlayTurn(playerId, card);

        assertEquals(playerId, playTurn.playerId(), "PlayerId should match the provided instance.");
        assertEquals(card, playTurn.playedCard(), "Card should match the provided instance.");
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when PlayerId is null")
    void testNullPlayerIdThrowsException() {
        Card card = new Card(Suit.HEARTS, Rank.ACE);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new PlayTurn(null, card)
        );

        assertEquals("Play turn: Player cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when Card is null")
    void testNullCardThrowsException() {
        PlayerId playerId = new PlayerId();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new PlayTurn(playerId, null)
        );

        assertEquals("Play turn: PlayedCard cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Validates auto-generated equality for the record")
    void testRecordEquality() {
        PlayerId playerId = new PlayerId();
        Card card = new Card(Suit.HEARTS, Rank.ACE);

        PlayTurn turn1 = new PlayTurn(playerId, card);
        PlayTurn turn2 = new PlayTurn(playerId, card);

        assertEquals(turn1, turn2, "Two records with identical data should be equal");
    }
}