package cli.events;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuEventsTest {

    @Test
    void amountOfBotsIOEvent_needsInput() {
        MenuEvents.AmountOfBotsIOEvent event = new MenuEvents.AmountOfBotsIOEvent();

        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(MenuEvents.class, event, "Event should implement MenuEvents");
    }

    @Test
    void botStrategyIOEvent_storesDataAndNeedsInput() {
        int expectedIndex = 2;
        MenuEvents.BotStrategyIOEvent event = new MenuEvents.BotStrategyIOEvent(expectedIndex);

        assertEquals(expectedIndex, event.botIndex(), "Should store the exact bot index");
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(MenuEvents.class, event, "Event should implement MenuEvents");
    }

    @Test
    void playerNameIOEvent_storesDataAndNeedsInput() {
        int expectedIndex = 1;
        MenuEvents.PlayerNameIOEvent event = new MenuEvents.PlayerNameIOEvent(expectedIndex);

        assertEquals(expectedIndex, event.playerIndex(), "Should store the exact player index");
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(MenuEvents.class, event, "Event should implement MenuEvents");
    }

    @Test
    void printNamesIOEvent_storesDataAndDoesNotNeedInput() {
        List<String> names = List.of("Alice", "Bob", "Charlie");
        MenuEvents.PrintNamesIOEvent event = new MenuEvents.PrintNamesIOEvent(names);

        assertEquals(names, event.playerNames(), "Should store the exact list of player names");
        assertFalse(event.needsInput(), "PrintNamesIOEvent should NOT need input");
        assertInstanceOf(MenuEvents.class, event, "Event should implement MenuEvents");
    }

    @Test
    void welcomeMenuIOEvent_needsInput() {
        MenuEvents.WelcomeMenuIOEvent event = new MenuEvents.WelcomeMenuIOEvent();

        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(MenuEvents.class, event, "Event should implement MenuEvents");
    }

    @Test
    void loadSaveIOEvent_storesDataAndNeedsInput() {
        List<String> saves = List.of("save1.json", "save2.json");
        MenuEvents.LoadSaveIOEvent event = new MenuEvents.LoadSaveIOEvent(saves);

        assertEquals(saves, event.availableSaves(), "Should store the exact list of available saves");
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(MenuEvents.class, event, "Event should implement MenuEvents");
    }
}