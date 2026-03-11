package base;

import base.domain.WhistGame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameControllerTest {
    private WhistGame testGame;

    @BeforeEach
    void setUp() {
        testGame = new WhistGame();
    }

    @Test
    void placeholderTest() {
        assertTrue(true);
    }
}