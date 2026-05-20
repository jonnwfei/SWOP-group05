package cli.util;

import cli.TerminalManager;
import cli.elements.Response;
import cli.events.CountEvents;
import cli.events.IOEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TerminalInputHelper")
class TerminalInputHelperTest {

    @Mock private TerminalManager terminalManager;

    private AutoCloseable mocks;
    private TerminalInputHelper helper;

    /** A simple concrete IOEvent that needs no extra arguments. */
    private final IOEvent event = new CountEvents.TrickInputIOEvent();

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        helper = new TerminalInputHelper(terminalManager);
    }

    @AfterEach
    void tearDown() throws Exception { mocks.close(); }

    private Response resp(String raw) {
        return new Response(raw);
    }

    // =========================================================================
    // askInt(event)
    // =========================================================================

    @Nested
    @DisplayName("askInt(event)")
    class AskIntTests {

        @Test
        @DisplayName("Returns parsed integer on valid input")
        void returnsOnValidInput() {
            when(terminalManager.handle(event)).thenReturn(resp("5"));
            assertEquals(5, helper.askInt(event));
        }

        @Test
        @DisplayName("Retries on non-numeric input, then returns on valid")
        void retriesOnInvalidThenReturns() {
            when(terminalManager.handle(event))
                    .thenReturn(resp("abc"))
                    .thenReturn(resp("3"));
            assertEquals(3, helper.askInt(event));
            verify(terminalManager, times(2)).handle(event);
        }

        @Test
        @DisplayName("Handles negative integers")
        void acceptsNegativeIntegers() {
            when(terminalManager.handle(event)).thenReturn(resp("-7"));
            assertEquals(-7, helper.askInt(event));
        }

        @Test
        @DisplayName("Trims whitespace before parsing")
        void trimsWhitespace() {
            when(terminalManager.handle(event)).thenReturn(resp("  42  "));
            assertEquals(42, helper.askInt(event));
        }
    }

    // =========================================================================
    // askInt(event, min, max)
    // =========================================================================

    @Nested
    @DisplayName("askInt(event, min, max)")
    class AskIntRangeTests {

        @Test
        @DisplayName("Returns immediately when value is within range")
        void inRangeReturnsImmediately() {
            when(terminalManager.handle(event)).thenReturn(resp("5"));
            assertEquals(5, helper.askInt(event, 1, 10));
        }

        @Test
        @DisplayName("Retries when value is below minimum")
        void retriesWhenBelowMin() {
            when(terminalManager.handle(event))
                    .thenReturn(resp("0"))
                    .thenReturn(resp("1"));
            assertEquals(1, helper.askInt(event, 1, 10));
            verify(terminalManager, times(2)).handle(event);
        }

        @Test
        @DisplayName("Retries when value is above maximum")
        void retriesWhenAboveMax() {
            when(terminalManager.handle(event))
                    .thenReturn(resp("11"))
                    .thenReturn(resp("10"));
            assertEquals(10, helper.askInt(event, 1, 10));
            verify(terminalManager, times(2)).handle(event);
        }

        @Test
        @DisplayName("Accepts boundary values min and max")
        void acceptsBoundaryValues() {
            when(terminalManager.handle(event)).thenReturn(resp("1"));
            assertEquals(1, helper.askInt(event, 1, 10));

            when(terminalManager.handle(event)).thenReturn(resp("10"));
            assertEquals(10, helper.askInt(event, 1, 10));
        }
    }

    // =========================================================================
    // askString(event)
    // =========================================================================

    @Nested
    @DisplayName("askString(event)")
    class AskStringTests {

        @Test
        @DisplayName("Returns trimmed non-blank input")
        void returnsTrimmedInput() {
            when(terminalManager.handle(event)).thenReturn(resp("  hello  "));
            assertEquals("hello", helper.askString(event));
        }

        @Test
        @DisplayName("Retries on blank input, then returns on valid")
        void retriesOnBlank() {
            when(terminalManager.handle(event))
                    .thenReturn(resp(""))
                    .thenReturn(resp("world"));
            assertEquals("world", helper.askString(event));
            verify(terminalManager, times(2)).handle(event);
        }

        @Test
        @DisplayName("Retries on whitespace-only input")
        void retriesOnWhitespace() {
            when(terminalManager.handle(event))
                    .thenReturn(resp("   "))
                    .thenReturn(resp("valid"));
            assertEquals("valid", helper.askString(event));
            verify(terminalManager, times(2)).handle(event);
        }
    }

    // =========================================================================
    // askIntList(event)
    // =========================================================================

    @Nested
    @DisplayName("askIntList(event)")
    class AskIntListTests {

        @Test
        @DisplayName("Parses comma-separated integers correctly")
        void parsesCommaSeparated() {
            when(terminalManager.handle(event)).thenReturn(resp("1,2,3"));
            assertEquals(List.of(1, 2, 3), helper.askIntList(event));
        }

        @Test
        @DisplayName("Returns empty list on blank input")
        void returnsEmptyOnBlank() {
            when(terminalManager.handle(event)).thenReturn(resp(""));
            assertEquals(List.of(), helper.askIntList(event));
        }

        @Test
        @DisplayName("Returns empty list on whitespace-only input")
        void returnsEmptyOnWhitespace() {
            when(terminalManager.handle(event)).thenReturn(resp("   "));
            assertEquals(List.of(), helper.askIntList(event));
        }

        @Test
        @DisplayName("Retries on non-numeric input, then returns on valid")
        void retriesOnNonNumeric() {
            when(terminalManager.handle(event))
                    .thenReturn(resp("abc,2"))
                    .thenReturn(resp("1,2"));
            assertEquals(List.of(1, 2), helper.askIntList(event));
            verify(terminalManager, times(2)).handle(event);
        }

        @Test
        @DisplayName("Handles single-element list")
        void singleElement() {
            when(terminalManager.handle(event)).thenReturn(resp("7"));
            assertEquals(List.of(7), helper.askIntList(event));
        }

        @Test
        @DisplayName("Trims whitespace around individual elements")
        void trimsElementWhitespace() {
            when(terminalManager.handle(event)).thenReturn(resp(" 1 , 2 , 3 "));
            assertEquals(List.of(1, 2, 3), helper.askIntList(event));
        }
    }
}
