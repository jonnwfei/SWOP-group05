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

        CountEvents.BidSelectionIOEvent event = new CountEvents.BidSelectionIOEvent(bids);

        assertArrayEquals(bids, event.bidTypes(), "Should store the exact BidType array");
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(CountEvents.class, event, "Event should implement CountEvents");
    }

    @Test
    void playerSelectionIOEvent_storesDataAndNeedsInput() {
        Player mockPlayer1 = mock(Player.class);
        Player mockPlayer2 = mock(Player.class);
        List<Player> players = List.of(mockPlayer1, mockPlayer2);

        CountEvents.PlayerSelectionIOEvent eventSingle = new CountEvents.PlayerSelectionIOEvent(players, false, BidType.SOLO);
        CountEvents.PlayerSelectionIOEvent eventMulti = new CountEvents.PlayerSelectionIOEvent(players, true, BidType.SOLO);

        assertEquals(players, eventSingle.players(), "Should store the exact player list");
        assertFalse(eventSingle.multi(), "Should store multi=false");
        assertTrue(eventSingle.needsInput(), "needsInput() must return true");

        assertTrue(eventMulti.multi(), "Should store multi=true");
        assertTrue(eventMulti.needsInput(), "needsInput() must return true");
    }

    @Test
    void saveDescriptionIOEvent_needsInput() {
        CountEvents.SaveDescriptionIOEvent event = new CountEvents.SaveDescriptionIOEvent();

        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(CountEvents.class, event, "Event should implement CountEvents");
    }

    @Test
    void scoreBoardIOEvent_storesDataAndNeedsInput() {
        List<String> names = List.of("Alice", "Bob");
        List<Integer> scores = List.of(100, 85);

        CountEvents.ScoreBoardIOEvent event = new CountEvents.ScoreBoardIOEvent(names, scores, false, false, false);

        assertEquals(names, event.playerNames(), "Should store the exact player names");
        assertEquals(scores, event.scores(), "Should store the exact scores");
        assertFalse(event.canRemovePlayer(), "Should store canRemovePlayer=false");
        assertFalse(event.canUndo(), "Should store canUndo=false");
        assertFalse(event.canRedo(), "Should store canRedo=false");
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(CountEvents.class, event, "Event should implement CountEvents");
    }

    @Test
    void trickInputIOEvent_needsInput() {
        CountEvents.TrickInputIOEvent event = new CountEvents.TrickInputIOEvent();

        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(CountEvents.class, event, "Event should implement CountEvents");
    }
}