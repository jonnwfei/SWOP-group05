package usecases;

import base.GameController;
import base.domain.WhistGame;
import base.domain.player.Player;
import org.junit.jupiter.api.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SaveAndResumeTest {
    private final InputStream sysInBackup = System.in;

    @AfterEach
    void tearDown() { System.setIn(sysInBackup); }

    private WhistGame runIntegrationTest(String... scriptLines) throws Exception {
        String script = String.join("\n", scriptLines) + "\n";
        System.setIn(new ByteArrayInputStream(script.getBytes()));
        GameController controller = new GameController();
        Field gameField = GameController.class.getDeclaredField("game");
        gameField.setAccessible(true);
        try { controller.run(); } catch (Exception ignored) {}
        return (WhistGame) gameField.get(controller);
    }

    @Test
    @DisplayName("UC 4.4 & 4.5: Full Save/Resume Life Cycle")
    void testSaveAndResumeProcess() throws Exception {
        // 1. Maak een game en voeg een score toe
        String description = "Session_To_Resume";
        WhistGame resumed = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4", // Count mode setup
                "1", "1", "1", "13",         // P1 wint alles (Solo Slim)
                "3", description,             // Optie 3: Save + omschrijving
                "2",                          // Optie 2: Exit naar men
                "3",            // Hoofdmenu: Resume
                "1",            // Selecteer de eerste beschikbare save
                "2"             // Exit
        );

        assertNotNull(resumed, "Resumed game should not be null");
        List<Player> players = resumed.getPlayers();
        assertEquals(4, players.size());

        // Controleer of de specifieke score van de bieder (P1) is opgeslagen
        // (Bij Solo Slim 13 slagen wint P1 flink wat punten)
        assertTrue(players.get(0).getScore() > 0, "Bidder score should be persisted");
        assertEquals(0, players.stream().mapToInt(Player::getScore).sum(), "Total sum must remain zero");
    }
}