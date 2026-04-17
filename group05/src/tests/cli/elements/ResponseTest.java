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
}