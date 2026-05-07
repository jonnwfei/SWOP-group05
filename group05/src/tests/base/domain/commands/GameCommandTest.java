package base.domain.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameCommand Architectural Tests")
class GameCommandTest {

    @Test
    @DisplayName("GameCommand must remain a strictly sealed interface")
    void mustBeSealedInterface() {
        assertTrue(GameCommand.class.isInterface(), "GameCommand must be an interface.");
        assertTrue(GameCommand.class.isSealed(), "GameCommand must be sealed to prevent unauthorized commands.");
    }

    @Test
    @DisplayName("GameCommand must strictly permit only the defined nested records")
    void strictlyPermittedSubclasses() {
        Class<?>[] permittedSubclasses = GameCommand.class.getPermittedSubclasses();
        assertNotNull(permittedSubclasses, "A sealed class must have permitted subclasses.");

        // Define the exact list of commands that are legally allowed to exist
        List<Class<?>> expectedClasses = List.of(
                GameCommand.BidCommand.class,
                GameCommand.CardCommand.class,
                GameCommand.NumberCommand.class,
                GameCommand.PlaceBidCommand.class,
                GameCommand.PlayerListCommand.class,
                GameCommand.RoundCommand.class,
                GameCommand.StartGameCommand.class,
                GameCommand.SuitCommand.class,
                GameCommand.TextCommand.class
        );

        assertEquals(expectedClasses.size(), permittedSubclasses.length,
                "The number of permitted subclasses has changed! Did you add/remove a command without updating the architecture test?");

        List<Class<?>> actualClasses = Arrays.asList(permittedSubclasses);
        assertTrue(actualClasses.containsAll(expectedClasses),
                "All defined command records must be officially recognized as permitted subclasses.");
    }
}