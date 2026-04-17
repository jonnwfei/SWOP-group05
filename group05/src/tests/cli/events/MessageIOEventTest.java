package cli.events;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageIOEventTest {

    @Test
    void messageIOEvent_needsInput_text() {
        // Arrange
        String input = "MessageIOEvent test";
        MessageIOEvent messageIOEvent = new MessageIOEvent(input);

        // Assert
        assertFalse(messageIOEvent.needsInput());
        assertEquals(input, messageIOEvent.text());
        assertInstanceOf(MessageIOEvent.class, messageIOEvent);
    }
}