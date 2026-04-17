package cli.events;

import base.domain.bid.BidType;
import base.domain.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CountEventsTest {

    @Test
    void bidSelectionIOEvent_storesDataAndNeedsInput() {
        BidType[] bids = {BidType.SOLO, BidType.PASS};

        // Act
        CountEvents.BidSelectionIOEvent event = new CountEvents.BidSelectionIOEvent(bids);

        // Assert
        assertArrayEquals(bids, event.bidTypes(), "Should store the exact BidType array");
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(CountEvents.class, event, "Event should implement CountEvents");
    }

    @Test
    void playerSelectionIOEvent_storesDataAndNeedsInput() {
        // Arrange
        Player mockPlayer1 = mock(Player.class);
        Player mockPlayer2 = mock(Player.class);
        List<Player> players = List.of(mockPlayer1, mockPlayer2);

        // Act
        CountEvents.PlayerSelectionIOEvent eventSingle = new CountEvents.PlayerSelectionIOEvent(players, false, BidType.SOLO);
        CountEvents.PlayerSelectionIOEvent eventMulti = new CountEvents.PlayerSelectionIOEvent(players, true, BidType.SOLO);

        // Assert
        assertEquals(players, eventSingle.players(), "Should store the exact player list");
        assertFalse(eventSingle.multi(), "Should store multi=false");
        assertTrue(eventSingle.needsInput(), "needsInput() must return true");

        assertTrue(eventMulti.multi(), "Should store multi=true");
        assertTrue(eventMulti.needsInput(), "needsInput() must return true");
    }

    @Test
    void saveDescriptionIOEvent_needsInput() {
        // Act
        CountEvents.SaveDescriptionIOEvent event = new CountEvents.SaveDescriptionIOEvent();

        // Assert
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(CountEvents.class, event, "Event should implement CountEvents");
    }

    @Test
    void scoreBoardIOEvent_storesDataAndNeedsInput() {
        // Arrange
        List<String> names = List.of("Alice", "Bob");
        List<Integer> scores = List.of(100, 85);

        // Act
        CountEvents.ScoreBoardIOEvent event = new CountEvents.ScoreBoardIOEvent(names, scores, false);

        // Assert
        assertEquals(names, event.playerNames(), "Should store the exact player names");
        assertEquals(scores, event.scores(), "Should store the exact scores");
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(CountEvents.class, event, "Event should implement CountEvents");
    }

    @Test
    void trickInputIOEvent_needsInput() {
        // Act
        CountEvents.TrickInputIOEvent event = new CountEvents.TrickInputIOEvent();

        // Assert
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(CountEvents.class, event, "Event should implement CountEvents");
    }
}