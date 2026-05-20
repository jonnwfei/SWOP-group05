package base;

import base.domain.WhistGame;
import base.domain.player.Player;
import base.domain.results.GameResult;
import base.domain.results.CountResults;
import base.domain.commands.GameCommand;
import base.domain.strategy.HumanStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameController Unit Tests")
class GameControllerTest {

    @Mock private WhistGame mockGame;
    private GameController controller;

    @BeforeEach
    void setUp() {
        controller = new GameController(mockGame);
    }

    @Test
    @DisplayName("Delegates advance to WhistGame")
    void testAdvance() {
        GameCommand command = new GameCommand.NumberCommand(1);
        GameResult expected = new CountResults.AmountOfTrickWonResult();
        when(mockGame.advance(command)).thenReturn(expected);

        GameResult result = controller.advance(command);

        assertEquals(expected, result);
        verify(mockGame).advance(command);
    }

    @Test
    @DisplayName("Delegates reset to WhistGame")
    void testReset() {
        controller.reset();
        verify(mockGame).resetPlayers();
        verify(mockGame).resetRounds();
    }

    @Test
    @DisplayName("Delegates player management to WhistGame")
    void testPlayerManagement() {
        Player player = mock(Player.class);

        controller.addPlayer(player);
        verify(mockGame).addPlayer(player);

        controller.removePlayer(player);
        verify(mockGame).removePlayer(player);

        when(mockGame.getAllPlayers()).thenReturn(List.of(player));
        assertEquals(List.of(player), controller.getPlayers());
    }

    @Test
    @DisplayName("Player factory methods add players correctly")
    void testPlayerFactories() {
        controller.addHumanPlayer("Alice");
        verify(mockGame).addPlayer(argThat(p -> p.getName().equals("Alice")));

        controller.addSmartBot("Bot1");
        verify(mockGame).addPlayer(argThat(p -> p.getName().equals("Bot1")));
    }

    @Test
    @DisplayName("Projection methods return data from WhistGame")
    void testProjections() {
        Player p1 = new Player(new HumanStrategy(), "Alice");
        p1.updateScore(10);
        when(mockGame.getAllPlayers()).thenReturn(List.of(p1));

        assertEquals(List.of("Alice"), controller.getPlayerNames());
        assertEquals(List.of(10), controller.getPlayerScores());
        assertEquals(1, controller.getPlayerCount());
    }
}
