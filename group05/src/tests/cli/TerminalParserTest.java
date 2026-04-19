package cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Terminal Parser Input Sanitization")
class TerminalParserTest {

    private TerminalParser parser;

    @BeforeEach
    void setUp() {
        parser = new TerminalParser();
    }

    @Nested
    @DisplayName("Single Number Parsing: parseNumberInput()")
    class ParseNumberInputTests {

        @ParameterizedTest(name = "Parses valid integer string: ''{0}'' -> {1}")
        @CsvSource({
                "5, 5",
                "-10, -10",
                "0, 0",
                "'  42  ', 42",  // Tests whitespace trimming
                "'\t99\n', 99"   // Tests tab/newline trimming
        })
        void parsesValidNumbers(String input, int expected) {
            assertEquals(expected, parser.parseNumberInput(input));
        }

        @ParameterizedTest(name = "Throws on null or blank input: ''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t"})
        void throwsOnNullOrBlank(String input) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> parser.parseNumberInput(input));

            assertEquals("Input cannot be empty.", ex.getMessage());
        }

        @ParameterizedTest(name = "Throws on non-numeric input: ''{0}''")
        @ValueSource(strings = {"abc", "5.5", "10a", "$5"})
        void throwsOnNonNumeric(String input) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> parser.parseNumberInput(input));

            assertEquals("'" + input + "' is not a number.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Multiple Numbers Parsing: parseNumbersInput()")
    class ParseNumbersInputTests {

        @Test
        @DisplayName("Parses standard comma-separated lists")
        void parsesCleanLists() {
            ArrayList<Integer> result = parser.parseNumbersInput("1, 2, 3");
            assertEquals(List.of(1, 2, 3), result);
        }

        @Test
        @DisplayName("Ignores empty segments and trims heavy whitespace")
        void parsesMessyLists() {
            // Contains leading/trailing spaces, consecutive commas, and trailing commas
            ArrayList<Integer> result = parser.parseNumbersInput("  1 , ,, 42, \t, 99 , ");
            assertEquals(List.of(1, 42, 99), result);
        }

        @ParameterizedTest(name = "Returns empty list for null or blank input: ''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t"})
        void returnsEmptyListOnNullOrBlank(String input) {
            ArrayList<Integer> result = parser.parseNumbersInput(input);
            assertTrue(result.isEmpty(), "Result should be an empty list.");
        }

        @Test
        @DisplayName("Throws exception if any single segment is non-numeric")
        void throwsOnInvalidSegment() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> parser.parseNumbersInput("1, abc, 3"));

            assertEquals("'abc' is not a number.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("String Parsing: parseString()")
    class ParseStringTests {

        @ParameterizedTest(name = "Trims valid strings: ''{0}'' -> ''{1}''")
        @CsvSource({
                "'hello', 'hello'",
                "'  hello world  ', 'hello world'",
                "'\ttext\n', 'text'",
                "'   ', ''" // A blank string should trim down to an empty string, not throw.
        })
        void trimsWhitespace(String input, String expected) {
            assertEquals(expected, parser.parseString(input));
        }

        @Test
        @DisplayName("Throws exception strictly on null input")
        void throwsOnNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> parser.parseString(null));

            assertEquals("Input cannot be null.", ex.getMessage());
        }
    }
}