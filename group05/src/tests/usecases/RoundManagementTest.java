package usecases;

import base.GameController;
import base.domain.WhistGame;
import base.domain.player.Player;
import org.junit.jupiter.api.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class RoundManagementTest {
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
    @DisplayName("UC 4.8: Remove Round and Revert Scores")
    void testRemoveRoundRevertsScores() throws Exception {
        // Voeg twee rondes toe met verschillende winnaars
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                "9", "1", "1","1" , // Ronde 1: P1 wint groot
                "9", "2", "2",   // Ronde 2: P2 wint groot

                // UC 4.8: Verwijder ronde 1
                "6", "1",             // Optie 6: Remove Round, Selecteer Ronde 1
                "2"                   // Exit
        );

        // Verifieer dat de score van P1 weer op 0 staat (omdat zijn enige winnende ronde weg is)
        // En dat de score van P2 nog wel punten bevat (Ronde 2)
        Player p1 = game.getPlayers().stream().filter(p -> p.getName().equals("P1")).findFirst().get();
        Player p2 = game.getPlayers().stream().filter(p -> p.getName().equals("P2")).findFirst().get();

        assertEquals(0, p1.getScore(), "Score of P1 should be reverted to zero after removing his winning round");
        assertNotEquals(0, p2.getScore(), "Score of P2 should remain unaffected by removing P1's round");
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum(), "Sum must remain zero-sum");
    }
}