package cli;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.round.Round;
import base.domain.turn.PlayTurn;
import cli.events.MessageIOEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cli.events.BidEvents.*;
import static cli.events.CountEvents.*;
import static cli.events.MenuEvents.*;
import static cli.events.PlayEvents.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TerminalRenderer Coverage Suite")
class TerminalRendererTest {

    private TerminalRenderer renderer;

    // Hijack System.out to suppress console spam during the test execution
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Mock private Card mockCard;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        renderer = new TerminalRenderer();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Play State Events - 100% Coverage")
    void testPlayStateEvents() {
        // ConfirmationIOEvent
        ConfirmationIOEvent confEvent = mock(ConfirmationIOEvent.class);
        when(confEvent.playerName()).thenReturn("Alice");
        renderer.render(confEvent);

        // PlayCardIOEvent (Branch 1: Empty Table, Not Open Miserie)
        PlayCardIOEvent pc1 = mock(PlayCardIOEvent.class, RETURNS_DEEP_STUBS);
        when(pc1.data().trickNumber()).thenReturn(1);
        when(pc1.data().player().getName()).thenReturn("Alice");
        when(pc1.data().turns()).thenReturn(Collections.emptyList());
        when(pc1.data().isOpenMiserie()).thenReturn(false);
        when(pc1.data().legalCards()).thenReturn(List.of(mockCard));
        renderer.render(pc1);

        // PlayCardIOEvent (Branch 2: Active Table, Open Miserie)
        PlayCardIOEvent pc2 = mock(PlayCardIOEvent.class, RETURNS_DEEP_STUBS);
        when(pc2.data().trickNumber()).thenReturn(1);
        when(pc2.data().player().getName()).thenReturn("Alice");

        PlayTurn turn = mock(PlayTurn.class);
        when(turn.playerId()).thenReturn(new PlayerId());
        when(turn.playedCard()).thenReturn(mockCard);

        when(pc2.data().turns()).thenReturn(List.of(turn));
        when(pc2.data().playerNames()).thenReturn(Map.of());
        when(pc2.data().isOpenMiserie()).thenReturn(true);
        when(pc2.data().exposedPlayerNames()).thenReturn(List.of("Bob"));
        when(pc2.data().formattedExposedHand()).thenReturn(List.of(List.of(mockCard)));
        when(pc2.data().legalCards()).thenReturn(List.of(mockCard));
        renderer.render(pc2);

        // EndOfTurnIOEvent
        EndOfTurnIOEvent eot = mock(EndOfTurnIOEvent.class, RETURNS_DEEP_STUBS);
        when(eot.data().name()).thenReturn("Alice");
        when(eot.data().card()).thenReturn(mockCard);
        renderer.render(eot);

        // EndOfTrickIOEvent
        EndOfTrickIOEvent eotr = mock(EndOfTrickIOEvent.class, RETURNS_DEEP_STUBS);
        when(eotr.data().name()).thenReturn("Alice");
        when(eotr.data().card()).thenReturn(mockCard);
        when(eotr.data().winner()).thenReturn("Bob");
        renderer.render(eotr);

        // EndOfRoundIOEvent
        EndOfRoundIOEvent eor = mock(EndOfRoundIOEvent.class, RETURNS_DEEP_STUBS);
        when(eor.data().name()).thenReturn("Alice");
        when(eor.data().card()).thenReturn(mockCard);
        renderer.render(eor);

        // TrickHistoryIOEvent (Branch 1: Incomplete trick)
        TrickHistoryIOEvent th1 = mock(TrickHistoryIOEvent.class, RETURNS_DEEP_STUBS);
        when(th1.data().trick().getTurns()).thenReturn(Collections.emptyList());
        when(th1.data().trick().isCompleted()).thenReturn(false);
        renderer.render(th1);

        // TrickHistoryIOEvent (Branch 2: Completed trick)
        TrickHistoryIOEvent th2 = mock(TrickHistoryIOEvent.class, RETURNS_DEEP_STUBS);
        when(th2.data().trick().getTurns()).thenReturn(List.of(turn)); // Reusing turn from above
        when(th2.data().playerNames()).thenReturn(Map.of());
        when(th2.data().trick().isCompleted()).thenReturn(true);
        when(th2.data().trick().getWinningPlayerId()).thenReturn(new PlayerId());
        renderer.render(th2);

        // ParticipatingPlayersIOEvent
        ParticipatingPlayersIOEvent pp = mock(ParticipatingPlayersIOEvent.class, RETURNS_DEEP_STUBS);
        when(pp.data().playerNames()).thenReturn(List.of("Alice"));
        renderer.render(pp);
    }

    @Test
    @DisplayName("Bid State Events - 100% Coverage")
    void testBidStateEvents() {
        // Because Bid is a sealed interface, Mockito's RETURNS_DEEP_STUBS will aggressively
        // try to analyze it and fail. The bulletproof solution is to use REAL data objects.

        base.domain.player.Player mockPlayer = mock(base.domain.player.Player.class);
        base.domain.player.PlayerId dummyId = new base.domain.player.PlayerId();
        base.domain.bid.Bid realBid = base.domain.bid.BidType.SOLO.instantiate(dummyId, base.domain.card.Suit.HEARTS);
        List<base.domain.card.Card> hand = List.of(mockCard);
        List<base.domain.bid.BidType> availableBids = List.of(base.domain.bid.BidType.PASS);

        // BidTurnIOEvent (Branch 1: No previous bids, null trump)
        base.domain.results.BidResults.BidTurnResult data1 = new base.domain.results.BidResults.BidTurnResult(
                "Alice", null, null, null, availableBids, hand, mockPlayer
        );
        renderer.render(new cli.events.BidEvents.BidTurnIOEvent(data1));

        // BidTurnIOEvent (Branch 2: Previous bid, no translated highest bidder name)
        base.domain.results.BidResults.BidTurnResult data2 = new base.domain.results.BidResults.BidTurnResult(
                "Alice", base.domain.card.Suit.HEARTS, realBid, "", availableBids, hand, mockPlayer
        );
        renderer.render(new cli.events.BidEvents.BidTurnIOEvent(data2));

        // BidTurnIOEvent (Branch 3: Previous bid, known highest bidder name)
        base.domain.results.BidResults.BidTurnResult data3 = new base.domain.results.BidResults.BidTurnResult(
                "Alice", base.domain.card.Suit.HEARTS, realBid, "Bob", availableBids, hand, mockPlayer
        );
        renderer.render(new cli.events.BidEvents.BidTurnIOEvent(data3));

        // Parameterless & simple events can also just be instantiated normally
        renderer.render(new cli.events.BidEvents.SuitSelectionIOEvent());
        renderer.render(new cli.events.BidEvents.BiddingCompletedIOEvent());

        // ProposalRejectedIOEvent
        base.domain.results.BidResults.ProposalRejected prData = new base.domain.results.BidResults.ProposalRejected("Alice");
        renderer.render(new cli.events.BidEvents.ProposalRejectedIOEvent(prData));
    }

    @Test
    @DisplayName("Count State Events - 100% Coverage")
    void testCountStateEvents() {
        // BidSelectionIOEvent
        BidSelectionIOEvent bs = mock(BidSelectionIOEvent.class);
        when(bs.bidTypes()).thenReturn(new BidType[]{BidType.SOLO});
        renderer.render(bs);

        // PlayerSelectionIOEvent (Test all 5 switch branches + lists)
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("Alice");

        PlayerSelectionIOEvent ps1 = mock(PlayerSelectionIOEvent.class);
        when(ps1.multi()).thenReturn(false); // Branch 1: Single select
        when(ps1.players()).thenReturn(List.of(mockPlayer));
        renderer.render(ps1);

        PlayerSelectionIOEvent ps2 = mock(PlayerSelectionIOEvent.class);
        when(ps2.multi()).thenReturn(true);
        when(ps2.type()).thenReturn(BidType.MISERIE); // Branch 2: Miserie
        when(ps2.players()).thenReturn(Collections.emptyList());
        renderer.render(ps2);

        PlayerSelectionIOEvent ps3 = mock(PlayerSelectionIOEvent.class);
        when(ps3.multi()).thenReturn(true);
        when(ps3.type()).thenReturn(BidType.PROPOSAL); // Branch 3: Proposal
        when(ps3.players()).thenReturn(Collections.emptyList());
        renderer.render(ps3);

        PlayerSelectionIOEvent ps4 = mock(PlayerSelectionIOEvent.class);
        when(ps4.multi()).thenReturn(true);
        when(ps4.type()).thenReturn(BidType.TROEL); // Branch 4: Troel
        when(ps4.players()).thenReturn(Collections.emptyList());
        renderer.render(ps4);

        PlayerSelectionIOEvent ps5 = mock(PlayerSelectionIOEvent.class);
        when(ps5.multi()).thenReturn(true);
        when(ps5.type()).thenReturn(BidType.SOLO); // Branch 5: Default fallback
        when(ps5.players()).thenReturn(Collections.emptyList());
        renderer.render(ps5);

        // ScoreBoardIOEvent (Testing boolean toggle)
        ScoreBoardIOEvent sb1 = mock(ScoreBoardIOEvent.class);
        when(sb1.playerNames()).thenReturn(List.of("Alice"));
        when(sb1.scores()).thenReturn(List.of(10));
        when(sb1.canRemovePlayer()).thenReturn(true);
        renderer.render(sb1);

        ScoreBoardIOEvent sb2 = mock(ScoreBoardIOEvent.class);
        when(sb2.playerNames()).thenReturn(List.of("Alice"));
        when(sb2.scores()).thenReturn(List.of(10));
        when(sb2.canRemovePlayer()).thenReturn(false);
        renderer.render(sb2);

        // Parameterless simple events
        renderer.render(mock(TrickInputIOEvent.class));
        renderer.render(mock(SaveDescriptionIOEvent.class));
    }

    @Test
    @DisplayName("Menu State Events - 100% Coverage")
    void testMenuEvents() {
        // Simple parameterless & primitive events
        renderer.render(mock(WelcomeMenuIOEvent.class));
        renderer.render(mock(AmountOfBotsIOEvent.class));
        renderer.render(mock(AddHumanPlayerIOEvent.class));
        renderer.render(mock(AddPlayerIOEvent.class));

        AmountOfHumansIOEvent humans = mock(AmountOfHumansIOEvent.class);
        when(humans.minHumans()).thenReturn(1);
        when(humans.maxHumans()).thenReturn(4);
        renderer.render(humans);

        PlayerNameIOEvent name = mock(PlayerNameIOEvent.class);
        when(name.playerIndex()).thenReturn(1);
        renderer.render(name);

        BotStrategyIOEvent bot = mock(BotStrategyIOEvent.class);
        when(bot.botIndex()).thenReturn(1);
        renderer.render(bot);

        PrintNamesIOEvent print = mock(PrintNamesIOEvent.class);
        when(print.playerNames()).thenReturn(List.of("Alice"));
        renderer.render(print);

        MessageIOEvent msg = mock(MessageIOEvent.class);
        when(msg.text()).thenReturn("Hello");
        renderer.render(msg);

        LoadSaveIOEvent load = mock(LoadSaveIOEvent.class);
        when(load.availableSaves()).thenReturn(List.of("Save 1"));
        renderer.render(load);

        // DeleteRoundIOEvent
        DeleteRoundIOEvent del = mock(DeleteRoundIOEvent.class);
        Round r = mock(Round.class);
        Player p = mock(Player.class);
        when(p.getName()).thenReturn("Alice");
        when(r.getWinningPlayers()).thenReturn(List.of(p));
        when(del.rounds()).thenReturn(List.of(r));
        renderer.render(del);
    }

    @Test
    @DisplayName("Unreachable Default Branch Guard")
    void testUnhandledEventThrows() {
        // Passing null forces the switch statement to throw a NullPointerException
        // before evaluating, because switch(event) null-checks its target natively.
        assertThrows(NullPointerException.class, () -> renderer.render(null));
    }
}