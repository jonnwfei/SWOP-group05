package usecases;


import base.GameController;
import base.domain.WhistGame;
import base.domain.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
public class InAppGame {
    private final InputStream sysInBackup = System.in;

    @AfterEach
    void tearDown() {
        System.setIn(sysInBackup);
    }


    private WhistGame runIntegrationTest(String... scriptLines) throws Exception {
        String script = String.join("\n", scriptLines) + "\n";
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        GameController controller = new GameController();

        // Gebruik reflectie om toegang te krijgen tot de private WhistGame game;
        Field gameField = GameController.class.getDeclaredField("game");
        gameField.setAccessible(true);
        WhistGame game = (WhistGame) gameField.get(controller);

        try {
            controller.run();
        } catch (Exception e) {
            // De controller stopt wanneer de gesimuleerde input op is.
        }
        return game;
    }

    @Test
    void mainSuccesCaseAbondance9() throws Exception {
        WhistGame game = runIntegrationTest(
                "1", "3", "John Doe", "1", "2", "1", "4", "2"
        );

    }
}
