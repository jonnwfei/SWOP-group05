package cli;

import cli.elements.Response;
import cli.events.IOEvent;
import cli.events.BidEvents.BiddingCompletedIOEvent;
import cli.events.CountEvents.TrickInputIOEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TerminalManager I/O Handling")
class TerminalManagerTest {

    @Mock private Scanner mockScanner;
    @Mock private TerminalRenderer mockRenderer;

    private TerminalManager terminalManager;

    @BeforeEach
    void setUp() throws Exception {
        // Instantiate the real class
        terminalManager = new TerminalManager();

        // Inject the mocked Scanner via Reflection
        Field scannerField = TerminalManager.class.getDeclaredField("scanner");
        scannerField.setAccessible(true);
        scannerField.set(terminalManager, mockScanner);

        // Inject the mocked Renderer via Reflection
        Field rendererField = TerminalManager.class.getDeclaredField("renderer");
        rendererField.setAccessible(true);
        rendererField.set(terminalManager, mockRenderer);
    }

    @Nested
    @DisplayName("Handle Event Logic")
    class HandleTests {

        @Test
        @DisplayName("When event needs input, it renders, reads, trims whitespace, and returns Response")
        void handlesEventRequiringInput() throws Exception {
            // Arrange: Use a REAL event that needs input (TrickInputIOEvent returns true)
            IOEvent inputEvent = new TrickInputIOEvent();

            // Simulate user typing with leading and trailing spaces
            when(mockScanner.nextLine()).thenReturn("   user input   ");

            // Act
            Response response = terminalManager.handle(inputEvent);

            // Assert
            verify(mockRenderer, times(1)).render(inputEvent);
            verify(mockScanner, times(1)).nextLine();

            assertNotNull(response, "Response should not be null");

            // Use reflection to verify the inner string of the Response object
            String responseValue = extractResponseValue(response);
            assertEquals("user input", responseValue, "Input should be properly trimmed of whitespace");
        }

        @Test
        @DisplayName("When event does NOT need input, it renders and returns an empty Response without pausing")
        void handlesEventNotRequiringInput() throws Exception {
            // Arrange: Use a REAL event that does NOT need input (BiddingCompletedIOEvent returns false)
            IOEvent noInputEvent = new BiddingCompletedIOEvent();

            // Act
            Response response = terminalManager.handle(noInputEvent);

            // Assert
            verify(mockRenderer, times(1)).render(noInputEvent);

            // Ensure the application doesn't freeze waiting for user input
            verify(mockScanner, never()).nextLine();

            assertNotNull(response);
            String responseValue = extractResponseValue(response);
            assertNull(responseValue, "Response internal value should be null when no input is required");
        }
    }

    /**
     * Helper method to extract the String from the Response object regardless of its architecture.
     */
    private String extractResponseValue(Response response) throws Exception {
        Field[] fields = response.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == String.class) {
                field.setAccessible(true);
                return (String) field.get(response);
            }
        }
        return null;
    }
}