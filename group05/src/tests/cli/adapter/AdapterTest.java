package cli.adapter;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.SoloBid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.results.*;
import base.domain.trick.Trick;
import base.domain.trick.Turn;
import cli.events.*;
import cli.events.BidEvents.*;
import cli.events.CountEvents.*;
import cli.events.PlayEvents.*;
import cli.elements.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdapterTest {

    private WhistGame game;
    private Player humanPlayer;
    private Player botPlayer;
    private Adapter adapter;

    private static final Card TEST_CARD = new Card(Suit.HEARTS, Rank.ACE);

    @BeforeEach
    void setUp() {
        game = mock(WhistGame.class);
        humanPlayer = mock(Player.class);
        botPlayer = mock(Player.class);

        adapter = new Adapter(game);

        lenient().when(humanPlayer.getRequiresConfirmation()).thenReturn(true);
        lenient().when(humanPlayer.getName()).thenReturn("Human");

        lenient().when(botPlayer.getRequiresConfirmation()).thenReturn(false);
        lenient().when(botPlayer.getName()).thenReturn("Bot");
    }

    // =========================================================================
    // handleResult() Tests
    // =========================================================================

    @Test
    void handleResult_playCard_botPlayer_passesLeadingSuitFromTableUsingCaptor() {
        Card leadCard = new Card(Suit.SPADES, Rank.KING);
        Turn turn = new Turn(botPlayer, leadCard);

        when(botPlayer.chooseCard(any())).thenReturn(TEST_CARD);

        PlayCardResult result = new PlayCardResult(
                List.of(turn), false, List.of(), List.of(),
                1, botPlayer, List.of(TEST_CARD), null
        );

        adapter.handleResult(result);

        ArgumentCaptor<Suit> suitCaptor = ArgumentCaptor.forClass(Suit.class);
        verify(botPlayer, times(1)).chooseCard(suitCaptor.capture());
        assertEquals(Suit.SPADES, suitCaptor.getValue());
    }

    @Test
    void handleResult_playCard_humanPlayer_returnsNeedsIOWithConfirmation() {
        PlayCardResult result = new PlayCardResult(
                List.of(), false, List.of(), List.of(),
                1, humanPlayer, List.of(TEST_CARD), null
        );

        AdapterResult adapterResult = adapter.handleResult(result);

        AdapterResult.NeedsIO needsIO = assertInstanceOf(AdapterResult.NeedsIO.class, adapterResult);
        assertInstanceOf(PlayCardIOEvent.class, needsIO.event());

        ConfirmationIOEvent preamble = assertInstanceOf(ConfirmationIOEvent.class, needsIO.preamble().getFirst());
        assertEquals("Human", preamble.playerName());
    }

    @Test
    void handleResult_bidTurn_botPlayer_returnsImmediateBidCommandAndUsesCaptor() {
        SoloBid mockBid = mock(SoloBid.class);
        when(mockBid.getType()).thenReturn(BidType.SOLO);
        when(mockBid.determineTrump(any())).thenReturn(Suit.HEARTS);
        when(botPlayer.chooseBid()).thenReturn(mockBid);

        BidTurnResult result = new BidTurnResult(
                "Bot", Suit.CLUBS, null,
                List.of(BidType.PASS, BidType.SOLO), List.of(TEST_CARD), botPlayer
        );

        AdapterResult adapterResult = adapter.handleResult(result);

        ArgumentCaptor<Suit> suitCaptor = ArgumentCaptor.forClass(Suit.class);
        verify(mockBid).determineTrump(suitCaptor.capture());
        assertEquals(Suit.CLUBS, suitCaptor.getValue());

        AdapterResult.Immediate immediateResult = assertInstanceOf(AdapterResult.Immediate.class, adapterResult);
        BidCommand command = assertInstanceOf(BidCommand.class, immediateResult.command());
        assertEquals(BidType.SOLO, command.bid());
    }

    @Test
    void handleResult_bidTurn_botPlayer_noTrumpRound_nonSuitBid_skipsTrumpResolution() {
        Bid mockBid = mock(Bid.class);
        when(mockBid.getType()).thenReturn(BidType.PASS);
        when(botPlayer.chooseBid()).thenReturn(mockBid);

        BidTurnResult result = new BidTurnResult(
                "Bot", null, null,
                List.of(BidType.PASS, BidType.MISERIE, BidType.OPEN_MISERIE), List.of(TEST_CARD), botPlayer
        );

        AdapterResult adapterResult = adapter.handleResult(result);

        verify(mockBid, never()).determineTrump(any());

        AdapterResult.Immediate immediateResult = assertInstanceOf(AdapterResult.Immediate.class, adapterResult);
        BidCommand command = assertInstanceOf(BidCommand.class, immediateResult.command());
        assertEquals(BidType.PASS, command.bid());
        assertNull(command.suit());
    }

    @Test
    void handleResult_bidTurn_botPlayer_noTrumpRound_suitBid_usesSafeTrumpForResolution() {
        SoloBid mockBid = mock(SoloBid.class);
        when(mockBid.getType()).thenReturn(BidType.SOLO);
        when(mockBid.determineTrump(any())).thenReturn(Suit.HEARTS);
        when(botPlayer.chooseBid()).thenReturn(mockBid);

        BidTurnResult result = new BidTurnResult(
                "Bot", null, null,
                List.of(BidType.PASS, BidType.SOLO), List.of(TEST_CARD), botPlayer
        );

        AdapterResult adapterResult = adapter.handleResult(result);

        ArgumentCaptor<Suit> suitCaptor = ArgumentCaptor.forClass(Suit.class);
        verify(mockBid).determineTrump(suitCaptor.capture());
        assertNotNull(suitCaptor.getValue());

        AdapterResult.Immediate immediateResult = assertInstanceOf(AdapterResult.Immediate.class, adapterResult);
        BidCommand command = assertInstanceOf(BidCommand.class, immediateResult.command());
        assertEquals(BidType.SOLO, command.bid());
        assertEquals(Suit.HEARTS, command.suit());
    }

    @Test
    void handleResult_bidTurn_humanPlayer_returnsNeedsIO() {
        BidTurnResult result = new BidTurnResult(
                "Human", Suit.CLUBS, null,
                List.of(BidType.PASS), List.of(TEST_CARD), humanPlayer
        );

        AdapterResult adapterResult = adapter.handleResult(result);
        assertNeedsIO(adapterResult, BidTurnIOEvent.class);
    }

    @Test
    void handleResult_allSimpleResults_returnNeedsIO() {
        // This test ensures 100% coverage over all the basic UI-mapping switches
        assertNeedsIO(adapter.handleResult(new SuitSelectionRequired("P", BidType.SOLO, Suit.values())), SuitSelectionIOEvent.class);
        assertNeedsIO(adapter.handleResult(new ProposalRejected("P")), ProposalRejectedIOEvent.class);
        assertNeedsIO(adapter.handleResult(new BiddingCompleted()), BiddingCompletedIOEvent.class);
        assertNeedsIO(adapter.handleResult(new BidSelectionResult(new BidType[]{BidType.SOLO},List.of(botPlayer,humanPlayer))), BidSelectionIOEvent.class);
        assertNeedsIO(adapter.handleResult(new SuitSelectionResult()), SuitSelectionIOEvent.class);
        assertNeedsIO(adapter.handleResult(new PlayerSelectionResult(List.of(botPlayer), false)), PlayerSelectionIOEvent.class);
        assertNeedsIO(adapter.handleResult(new AmountOfTrickWonResult()), TrickInputIOEvent.class);
        assertNeedsIO(adapter.handleResult(new SaveDescriptionResult()), SaveDescriptionIOEvent.class);
        assertNeedsIO(adapter.handleResult(new ScoreBoardResult(List.of("A"), List.of(1))), ScoreBoardIOEvent.class);
        assertNeedsIO(adapter.handleResult(new EndOfTurnResult("A", TEST_CARD)), EndOfTurnIOEvent.class);
        assertNeedsIO(adapter.handleResult(new EndOfTrickResult("A", TEST_CARD, "B")), EndOfTrickIOEvent.class);
        assertNeedsIO(adapter.handleResult(new EndOfRoundResult("A", TEST_CARD)), EndOfRoundIOEvent.class);
        assertNeedsIO(adapter.handleResult(new TrickHistoryResult(mock(Trick.class))), TrickHistoryIOEvent.class);
        assertNeedsIO(adapter.handleResult(new ParticipatingPlayersResult(List.of("A"), false)), ParticipatingPlayersIOEvent.class);
    }

    // =========================================================================
    // handleResponse() Tests
    // =========================================================================

    @Test
    void handleResponse_emptyInput_returnsNullCommand() {
        AdapterResponse response = adapter.handleResponse(new Response("   "), new BiddingCompleted());
        assertNull(response.command());
        assertFalse(response.shouldReRenderLastResult());
    }

    @Test
    void handleResponse_bidTurn_validInput_returnsBidCommand() {
        BidTurnResult result = new BidTurnResult("Human", Suit.HEARTS, null, List.of(BidType.PASS, BidType.SOLO), List.of(TEST_CARD), humanPlayer);
        AdapterResponse response = adapter.handleResponse(new Response("2"), result);

        BidCommand command = assertInstanceOf(BidCommand.class, response.command());
        assertEquals(BidType.SOLO, command.bid());
    }

    @Test
    void handleResponse_suitSelection_validChoice_returnsSuitCommand() {
        SuitSelectionRequired result = new SuitSelectionRequired("Player1", BidType.SOLO, Suit.values());
        AdapterResponse response = adapter.handleResponse(new Response("1"), result);

        SuitCommand command = assertInstanceOf(SuitCommand.class, response.command());
        assertEquals(Suit.values()[0], command.suit());
    }

    @Test
    void handleResponse_proposalRejected_mapsCorrectly() {
        ProposalRejected result = new ProposalRejected("P");

        // Choice 1 -> PASS
        AdapterResponse response1 = adapter.handleResponse(new Response("1"), result);
        assertEquals(BidType.PASS, assertInstanceOf(BidCommand.class, response1.command()).bid());

        // Choice 2 -> SOLO_PROPOSAL
        AdapterResponse response2 = adapter.handleResponse(new Response("2"), result);
        assertEquals(BidType.SOLO_PROPOSAL, assertInstanceOf(BidCommand.class, response2.command()).bid());
    }

    @Test
    void handleResponse_biddingCompleted_returnsNullCommand() {
        AdapterResponse response = adapter.handleResponse(new Response("enter"), new BiddingCompleted());
        assertNull(response.command());
    }

    @Test
    void handleResponse_variousDataResults_mapsToCommands() {
        // Bid Selection
        AdapterResponse bidRes = adapter.handleResponse(new Response("1"), new BidSelectionResult(new BidType[]{BidType.SOLO}, List.of(botPlayer,humanPlayer) ));
        assertEquals(BidType.SOLO, assertInstanceOf(BidCommand.class, bidRes.command()).bid());

        // Suit Selection
        AdapterResponse suitRes = adapter.handleResponse(new Response("2"), new SuitSelectionResult());
        assertEquals(Suit.values()[1], assertInstanceOf(SuitCommand.class, suitRes.command()).suit());

        // Amount of Tricks Won
        AdapterResponse trickRes = adapter.handleResponse(new Response("5"), new AmountOfTrickWonResult());
        assertEquals(5, assertInstanceOf(NumberCommand.class, trickRes.command()).choice());

        // Score Board
        AdapterResponse scoreRes = adapter.handleResponse(new Response("3"), new ScoreBoardResult(List.of("dummy name"), List.of(0)));
        assertEquals(3, assertInstanceOf(NumberCommand.class, scoreRes.command()).choice());

        // Save Description
        AdapterResponse saveRes = adapter.handleResponse(new Response("My Save"), new SaveDescriptionResult());
        assertEquals("My Save", assertInstanceOf(TextCommand.class, saveRes.command()).text());
    }

    @Test
    void handleResponse_playerSelection_mapsZeroToEmptyListAndNumbersToPlayers() {
        Player p1 = mock(Player.class);
        when(game.getPlayers()).thenReturn(List.of(p1));
        PlayerSelectionResult result = new PlayerSelectionResult(List.of(p1), true);

        // Input 0
        AdapterResponse response0 = adapter.handleResponse(new Response("0"), result);
        assertTrue(assertInstanceOf(PlayerListCommand.class, response0.command()).players().isEmpty());

        // Input 1
        AdapterResponse response1 = adapter.handleResponse(new Response("1"), result);
        assertEquals(p1, assertInstanceOf(PlayerListCommand.class, response1.command()).players().getFirst());
    }

    @Test
    void handleResponse_participatingPlayers_mapsNamesToPlayersCorrectly() {
        Player p1 = mock(Player.class);
        when(p1.getName()).thenReturn("Alice");
        Player p2 = mock(Player.class);
        when(p2.getName()).thenReturn("Bob");
        when(game.getPlayers()).thenReturn(List.of(p1, p2));

        ParticipatingPlayersResult result = new ParticipatingPlayersResult(List.of("Alice", "Bob"), true);
        AdapterResponse response = adapter.handleResponse(new Response("2"), result);

        PlayerListCommand command = assertInstanceOf(PlayerListCommand.class, response.command());
        assertEquals(p2, command.players().getFirst());
    }

    @Test
    void handleResponse_flowEvents_returnsNullCommand() {
        assertNull(adapter.handleResponse(new Response("go"), new EndOfTurnResult("A", TEST_CARD)).command());
        assertNull(adapter.handleResponse(new Response("go"), new EndOfTrickResult("A", TEST_CARD, "B")).command());
        assertNull(adapter.handleResponse(new Response("go"), new EndOfRoundResult("A", TEST_CARD)).command());
        assertNull(adapter.handleResponse(new Response("go"), new TrickHistoryResult(mock(Trick.class))).command());
    }

    @Test
    void handleResponse_playCard_botPlayer_autoPlaysAndReturnsCommandWithCaptor() {
        Card leadCard = new Card(Suit.DIAMONDS, Rank.QUEEN);
        Turn turn = new Turn(botPlayer, leadCard);
        when(botPlayer.chooseCard(any())).thenReturn(TEST_CARD);

        PlayCardResult result = new PlayCardResult(
                List.of(turn), false, List.of(), List.of(),
                1, botPlayer, List.of(TEST_CARD), null
        );

        AdapterResponse response = adapter.handleResponse(new Response("ignore me"), result);

        ArgumentCaptor<Suit> suitCaptor = ArgumentCaptor.forClass(Suit.class);
        verify(botPlayer).chooseCard(suitCaptor.capture());
        assertEquals(Suit.DIAMONDS, suitCaptor.getValue());

        CardCommand command = assertInstanceOf(CardCommand.class, response.command());
        assertEquals(TEST_CARD, command.card());
    }

    @Test
    void handleResponse_playCard_humanPlayer_inputZeroNoTrick_returnsUiOnly() {
        PlayCardResult result = new PlayCardResult(
                List.of(), false, List.of(), List.of(),
                1, humanPlayer, List.of(TEST_CARD), null // Null last trick
        );

        AdapterResponse response = adapter.handleResponse(new Response("0"), result);

        assertNull(response.command());
        assertTrue(response.shouldReRenderLastResult());
        MessageIOEvent msg = assertInstanceOf(MessageIOEvent.class, response.immediateEvents().getFirst());
        assertTrue(msg.text().contains("No tricks"));
    }

    @Test
    void handleResponse_playCard_humanPlayer_inputZeroWithTrick_returnsTrickHistoryIOEvent() {
        Trick mockTrick = mock(Trick.class);
        PlayCardResult result = new PlayCardResult(
                List.of(), false, List.of(), List.of(),
                1, humanPlayer, List.of(TEST_CARD), mockTrick
        );

        AdapterResponse response = adapter.handleResponse(new Response("0"), result);

        assertNull(response.command());
        assertTrue(response.shouldReRenderLastResult());
        assertInstanceOf(TrickHistoryIOEvent.class, response.immediateEvents().getFirst());
    }

    @Test
    void handleResponse_playCard_humanPlayer_invalidChoiceOutOfBounds_returnsUiOnly() {
        PlayCardResult result = new PlayCardResult(
                List.of(), false, List.of(), List.of(),
                1, humanPlayer, List.of(TEST_CARD), null
        );

        AdapterResponse response = adapter.handleResponse(new Response("5"), result); // 5 is out of bounds

        assertNull(response.command());
        assertTrue(response.shouldReRenderLastResult());
        MessageIOEvent msg = assertInstanceOf(MessageIOEvent.class, response.immediateEvents().getFirst());
        assertTrue(msg.text().contains("Invalid selection"));
    }

    @Test
    void handleResponse_playCard_humanPlayer_validChoice_returnsCardCommand() {
        PlayCardResult result = new PlayCardResult(
                List.of(), false, List.of(), List.of(),
                1, humanPlayer, List.of(TEST_CARD), null
        );

        AdapterResponse response = adapter.handleResponse(new Response("1"), result);

        CardCommand command = assertInstanceOf(CardCommand.class, response.command());
        assertEquals(TEST_CARD, command.card());
    }

    @Test
    void handleResponse_invalidInputCatchBlock_returnsUiOnlyMessage() {
        SuitSelectionRequired result = new SuitSelectionRequired("Player1", BidType.SOLO, Suit.values());

        // Simulating bad input that throws a NumberFormatException internally
        AdapterResponse response = adapter.handleResponse(new Response("not_a_number"), result);

        assertNull(response.command());
        assertTrue(response.shouldReRenderLastResult());
        MessageIOEvent messageEvent = assertInstanceOf(MessageIOEvent.class, response.immediateEvents().getFirst());
        assertTrue(messageEvent.text().contains("Invalid input"));
    }

    @Test
    void handleResponse_unsupportedGameResult_triggersCatchBlockError() {
        AdapterResponse response = adapter.handleResponse(new Response("1"), null);

        assertNull(response.command());
        assertTrue(response.shouldReRenderLastResult());
        MessageIOEvent messageEvent = assertInstanceOf(MessageIOEvent.class, response.immediateEvents().getFirst());
        assertTrue(messageEvent.text().contains("Invalid input"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void assertNeedsIO(AdapterResult result, Class<? extends IOEvent> expectedEventClass) {
        AdapterResult.NeedsIO needsIO = assertInstanceOf(AdapterResult.NeedsIO.class, result);
        assertInstanceOf(expectedEventClass, needsIO.event());
    }
}