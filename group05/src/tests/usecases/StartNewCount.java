package usecases;

import base.domain.WhistGame;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import base.domain.states.CountState;
import base.domain.states.MenuState;
import base.domain.states.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class StartNewCount {

    private WhistGame game;
    private final InputStream sysInBackup = System.in;

    @BeforeEach
    void setUp() {
        game = new WhistGame();
    }

    @AfterEach
    void tearDown() {
        System.setIn(sysInBackup);
    }

    /**
     * Hulpmethode die een reeks inputs (gescheiden door newlines) in System.in zet
     * en de state-machine doorloopt tot de inputs op zijn.
     */
    private void runFullSequence(String... inputs) {
        String joinedInput = String.join("\n", inputs) + "\n";
        System.setIn(new ByteArrayInputStream(joinedInput.getBytes()));
        Scanner scanner = new Scanner(System.in);

        State currentState = new MenuState(game);

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine();
            currentState.executeState(input);

            // Check of we naar een volgende staat moeten (bijv. na namen invoeren of ronde afronden)
            State next = currentState.nextState();
            if (next != currentState) {
                currentState = next;
                // De start-fase van een nieuwe state triggeren indien nodig
                currentState.executeState("");
            }
        }
    }

    @Test
    void testFullScenarioAbondance() {
        // Script:
        // 1. Kies Count (2)
        // 2. Namen: John 1, John 2, John 3, John 4
        // 3. Bid: Abondance 9 (3)
        // 4. Trump: Clubs (2)
        // 5. Player: John 2 (2) -> index 1
        // 6. Slagen: 10
        // 7. Stop: Menu (2)
        runFullSequence("2", "John 1", "John 2", "John 3", "John 4", "3", "2", "2", "10");

        assertEquals(4, game.getPlayers().size());
        assertEquals(1, game.getRounds().size());
        assertTrue(game.getPlayers().get(1).getScore() > 0, "Bieder John 2 moet punten hebben");
    }

    @Test
    void testFullScenarioMiserie() {
        // Eerst handmatig spelers toevoegen voor een schone CountState test
        game.addPlayer(new Player(new HumanStrategy(), "P1"));
        game.addPlayer(new Player(new HumanStrategy(), "P2"));
        game.addPlayer(new Player(new HumanStrategy(), "P3"));
        game.addPlayer(new Player(new HumanStrategy(), "P4"));

        // Script: Start, Miserie (7), Trump (1), Spelers P1&P2 (1,2), Winnaar P1 (1), Stop (2)
        // We omzeilen MenuState door direct een CountState te starten in de sequence helper
        String joinedInput = String.join("\n", "7", "1", "1, 2", "1", "2") + "\n";
        System.setIn(new ByteArrayInputStream(joinedInput.getBytes()));
        Scanner scanner = new Scanner(System.in);

        State currentState = new CountState(game);
        currentState.executeState(""); // De initiële vraag tonen

        while (scanner.hasNextLine()) {
            currentState.executeState(scanner.nextLine());
            currentState = currentState.nextState();
        }

        assertTrue(game.getPlayers().get(0).getScore() > 0, "P1 wint");
        assertTrue(game.getPlayers().get(1).getScore() < 0, "P2 verliest");
    }

    @Test
    void testSequentialRounds() {
        // Scenario: 2 rondes achter elkaar tellen zonder te stoppen
        // Menu(2) -> Namen -> Bid(3), Trump(2), Speler(2), Slagen(10) -> Opnieuw(1) -> Bid(7)...
        runFullSequence("2", "P1", "P2", "P3", "P4",
                "3", "2", "2", "10", "1", // Eerste ronde + "Simulate another"
                "7", "1", "1", "1", "2"); // Tweede ronde (Miserie) + "Back to menu"

        assertEquals(2, game.getRounds().size());
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum(), "Zero-sum check");
    }

    @Test
    void testSoloSlimAndZeroSum() {
        runFullSequence("2", "A", "B", "C", "D", "10", "1", "1", "13", "2");

        int total = game.getPlayers().stream().mapToInt(Player::getScore).sum();
        assertEquals(0, total);
        assertTrue(game.getPlayers().get(0).getScore() > 0);
    }
}