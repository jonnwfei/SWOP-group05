package cli.events;

import base.domain.card.Card;
import base.domain.results.BidResults.*;
import base.domain.results.CountResults.*;
import base.domain.results.PlayResults.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PlayEventsTest {

    @Test
    void confirmationIOEvent_storesDataAndNeedsInput() {
        String expectedName = "Alice";
        PlayEvents.ConfirmationIOEvent event = new PlayEvents.ConfirmationIOEvent(expectedName);

        assertEquals(expectedName, event.playerName(), "Should store the exact player name");
        assertTrue(event.needsInput(), "ConfirmationIOEvent must return true");
        assertInstanceOf(PlayEvents.class, event, "Event should implement PlayEvents");
    }

    @Test
    void endOfRoundIOEvent_storesDataAndDoesNotNeedInput() {
        EndOfRoundResult mockResult = mock(EndOfRoundResult.class);
        PlayEvents.EndOfRoundIOEvent event = new PlayEvents.EndOfRoundIOEvent(mockResult);

        assertEquals(mockResult, event.data(), "Should store the exact EndOfRoundResult");
        assertFalse(event.needsInput(), "EndOfRoundIOEvent should NOT need input");
        assertInstanceOf(PlayEvents.class, event, "Event should implement PlayEvents");
    }

    @Test
    void endOfTrickIOEvent_storesDataAndDoesNotNeedInput() {
        EndOfTrickResult mockResult = mock(EndOfTrickResult.class);
        PlayEvents.EndOfTrickIOEvent event = new PlayEvents.EndOfTrickIOEvent(mockResult);

        assertEquals(mockResult, event.data(), "Should store the exact EndOfTrickResult");
        assertFalse(event.needsInput(), "EndOfTrickIOEvent should NOT need input");
        assertInstanceOf(PlayEvents.class, event, "Event should implement PlayEvents");
    }

    @Test
    void endOfTurnIOEvent_storesDataAndDoesNotNeedInput() {
        EndOfTurnResult mockResult = mock(EndOfTurnResult.class);
        PlayEvents.EndOfTurnIOEvent event = new PlayEvents.EndOfTurnIOEvent(mockResult);

        assertEquals(mockResult, event.data(), "Should store the exact EndOfTurnResult");
        assertFalse(event.needsInput(), "EndOfTurnIOEvent should NOT need input");
        assertInstanceOf(PlayEvents.class, event, "Event should implement PlayEvents");
    }

    @Test
    void participatingPlayersIOEvent_storesDataAndNeedsInput() {
        ParticipatingPlayersResult mockResult = mock(ParticipatingPlayersResult.class);
        PlayEvents.ParticipatingPlayersIOEvent event = new PlayEvents.ParticipatingPlayersIOEvent(mockResult);

        assertEquals(mockResult, event.data(), "Should store the exact ParticipatingPlayersResult");
        assertTrue(event.needsInput(), "ParticipatingPlayersIOEvent must return true");
        assertInstanceOf(PlayEvents.class, event, "Event should implement PlayEvents");
    }

    @Test
    void playCardIOEvent_storesDataAndNeedsInput() {
        PlayCardResult mockResult = mock(PlayCardResult.class);
        PlayEvents.PlayCardIOEvent event = new PlayEvents.PlayCardIOEvent(mockResult);

        assertEquals(mockResult, event.data(), "Should store the exact PlayCardResult");
        assertTrue(event.needsInput(), "PlayCardIOEvent must return true");
        assertInstanceOf(PlayEvents.class, event, "Event should implement PlayEvents");
    }

    @Test
    void trickHistoryIOEvent_storesDataAndDoesNotNeedInput() {
        TrickHistoryResult mockResult = mock(TrickHistoryResult.class);
        PlayEvents.TrickHistoryIOEvent event = new PlayEvents.TrickHistoryIOEvent(mockResult);

        assertEquals(mockResult, event.data(), "Should store the exact TrickHistoryResult");
        assertFalse(event.needsInput(), "TrickHistoryIOEvent should NOT need input");
        assertInstanceOf(PlayEvents.class, event, "Event should implement PlayEvents");
    }
}