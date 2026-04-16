package usecases;

import base.GameController;
import base.domain.WhistGame;
import base.domain.player.Player;
import org.junit.jupiter.api.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerManagementTest {
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
    @DisplayName("UC 4.6 & 4.7: Adding Bots and Enforcing Player Minimums")
    void testPlayerFlow() throws Exception {
        // Start met 4 spelers in Count mode
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                "1", "1", "1", "7",   // Initial round to get to Scoreboard

                // UC 4.6: Voeg een bot toe (Extensie 2a)
                "4", "SmartBot", "3", // Optie 4: Add, Naam: SmartBot, Type: Smart (3)

                // UC 4.7: Verwijder een speler
                "5", "1",             // Optie 5: Remove, Selecteer P1 (1)

                // UC 4.7 Extensie 3a: Probeer onder de 4 spelers te gaan
                "5", "1",             // Probeer P2 te verwijderen (zou geblokkeerd moeten worden)
                "2"                   // Exit
        );

        // Verificatie
        assertEquals(4, game.getPlayers().size(), "Should never drop below 4 players");
        assertTrue(game.getPlayers().stream().anyMatch(p -> p.getName().equals("SmartBot")), "Bot should be present");
        assertFalse(game.getPlayers().stream().anyMatch(p -> p.getName().equals("P1")), "P1 should be removed");
    }
}