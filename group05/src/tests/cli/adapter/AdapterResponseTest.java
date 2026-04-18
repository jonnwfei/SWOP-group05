package cli.adapter;

import base.domain.commands.GameCommand;
import base.domain.commands.GameCommand.*;
import cli.events.BidEvents;
import cli.events.IOEvent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AdapterResponseTest {

    @Test
    void constructor_validInputs_createsInstance() {
        // Arrange with random GameCommand and IOEvent classes for mockito
        GameCommand mockCommand = mock(StartGameCommand.class);
        IOEvent mockEvent = mock(BidEvents.BiddingCompletedIOEvent.class);
        List<IOEvent> events = List.of(mockEvent);

        // Act
        AdapterResponse response = new AdapterResponse(mockCommand, events, true);

        // Assert
        assertEquals(mockCommand, response.command());
        assertEquals(events, response.immediateEvents());
        assertTrue(response.shouldReRenderLastResult());
    }

    @Test
    void constructor_nullInEventsList_throwsIllegalArgumentException() {
        // Arrange with random GameCommand and IOEvent classes for mockito
        GameCommand mockCommand = mock(StartGameCommand.class);
        IOEvent mockEvent = mock(BidEvents.BiddingCompletedIOEvent.class);

        // We MUST use Arrays.asList because List.of() natively throws NPE on nulls
        // before our constructor even gets a chance to run its validation logic.
        List<IOEvent> listWithNull = Arrays.asList(mockEvent, null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new AdapterResponse(mockCommand, listWithNull, false)
        );

        assertEquals("Immediate Event list must not contain null elements", exception.getMessage());
    }

    @Test
    void toDomain_withValidCommand_createsCorrectResponse() {
        // Arrange with random GameCommand and IOEvent classes for mockito
        GameCommand mockCommand = mock(StartGameCommand.class);

        // Act
        AdapterResponse response = AdapterResponse.toDomain(mockCommand);

        // Assert
        assertEquals(mockCommand, response.command());
        assertTrue(response.immediateEvents().isEmpty(), "Immediate events list should be empty");
        assertFalse(response.shouldReRenderLastResult(), "Should not re-render last result");
    }

    @Test
    void toDomain_withNullCommand_createsCorrectResponse() {
        // Act
        AdapterResponse response = AdapterResponse.toDomain(null);

        // Assert
        assertNull(response.command());
        assertTrue(response.immediateEvents().isEmpty());
        assertFalse(response.shouldReRenderLastResult());
    }

    @Test
    void uiOnly_withMultipleEvents_createsCorrectResponse() {
        // Arrange with random GameCommand and IOEvent classes for mockito
        IOEvent event1 = mock(BidEvents.BiddingCompletedIOEvent.class);
        IOEvent event2 = mock(BidEvents.BiddingCompletedIOEvent.class);

        // Act
        AdapterResponse response = AdapterResponse.uiOnly(event1, event2);

        // Assert
        assertNull(response.command(), "Command should be null for UI-only responses");
        assertEquals(List.of(event1, event2), response.immediateEvents());
        assertTrue(response.shouldReRenderLastResult(), "Should re-render last result");
    }

    @Test
    void uiOnly_withNoEvents_createsCorrectResponse() {
        // Act
        AdapterResponse response = AdapterResponse.uiOnly();

        // Assert
        assertNull(response.command());
        assertTrue(response.immediateEvents().isEmpty());
        assertTrue(response.shouldReRenderLastResult());
    }
}