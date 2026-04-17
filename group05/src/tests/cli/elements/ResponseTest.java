package cli.elements;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {
    @Test
    void responseTest_rawInput() {
        // Arrange
        String rawInput = "test";
        Response response = new Response(rawInput);

        assertEquals(rawInput, response.rawInput(), "Response should store the exact rawInput string");
    }

    @Test
    void responseTest_constructorDefensiveness() {
        assertThrows(IllegalArgumentException.class, () -> new Response(null),
                "Constructor should throw IllegalArgumentException when rawInput is null");
    }
}