package cli.adapter;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.results.*;
import base.domain.trick.Trick;
import cli.elements.Response;
import cli.events.*;
import cli.events.BidEvents.*;
import cli.events.PlayEvents.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdapterTest {

    @Mock WhistGame game;
    @Mock Player humanPlayer;
    @Mock Player botPlayer;

    private Adapter adapter;

    private static final Card TEST_CARD = new Card(Suit.HEARTS, Rank.ACE);

    @BeforeEach
    void setUp() {
        adapter = new Adapter(game);

        lenient().when(humanPlayer.getRequiresConfirmation()).thenReturn(true);
        lenient().when(botPlayer.getRequiresConfirmation()).thenReturn(false);
    }



    // =========================================================================
    // handleResult — PlayCardResult
    // =========================================================================

    @Test
    void handleResult_playCard_humanPlayer_returnsNeedsIO() {
        PlayCardResult result = playCardResult(humanPlayer);

        AdapterResult adapterResult = adapter.handleResult(result);

        assertThat(adapterResult).isInstanceOf(AdapterResult.NeedsIO.class);
        assertThat(((AdapterResult.NeedsIO) adapterResult).event())
                .isInstanceOf(PlayCardIOEvent.class);
    }

    @Test
    void handleResult_playCard_botPlayer_returnsImmediateCardCommand() {
        when(botPlayer.chooseCard(any())).thenReturn(TEST_CARD);
        PlayCardResult result = playCardResult(botPlayer);

        AdapterResult adapterResult = adapter.handleResult(result);

        assertThat(adapterResult).isInstanceOf(AdapterResult.Immediate.class);
        GameCommand command = ((AdapterResult.Immediate) adapterResult).command();
        assertThat(command).isInstanceOf(CardCommand.class);
        assertThat(((CardCommand) command).card()).isEqualTo(TEST_CARD);
    }

    @Test
    void handleResult_playCard_botPlayer_passesLeadingSuitFromTable() {
        Card leadCard = new Card(Suit.SPADES, Rank.KING);
        when(botPlayer.chooseCard(Suit.SPADES)).thenReturn(TEST_CARD);

        PlayCardResult result = new PlayCardResult(
                List.of(leadCard), false, List.of(), List.of(),
                1, botPlayer, List.of(TEST_CARD), null
        );

        adapter.handleResult(result);

        verify(botPlayer).chooseCard(Suit.SPADES);
    }

    @Test
    void handleResult_playCard_botPlayer_passesNullSuitWhenTableEmpty() {
        when(botPlayer.chooseCard(null)).thenReturn(TEST_CARD);

        PlayCardResult result = new PlayCardResult(
                List.of(), false, List.of(), List.of(),
                1, botPlayer, List.of(TEST_CARD), null
        );

        adapter.handleResult(result);

        verify(botPlayer).chooseCard(null);
    }

    // =========================================================================
    // handleResult — BidTurnResult
    // =========================================================================

    @Test
    void handleResult_bidTurn_humanPlayer_returnsNeedsIO() {
        BidTurnResult result = bidTurnResult(humanPlayer);

        AdapterResult adapterResult = adapter.handleResult(result);

        assertThat(adapterResult).isInstanceOf(AdapterResult.NeedsIO.class);
        assertThat(((AdapterResult.NeedsIO) adapterResult).event())
                .isInstanceOf(BidTurnIOEvent.class);
    }


    // =========================================================================
    // handleResult — NeedsIO passthrough cases
    // =========================================================================

    @Test
    void handleResult_suitSelectionRequired_returnsNeedsIO() {
        SuitSelectionRequired result = new SuitSelectionRequired("Player1", BidType.SOLO, Suit.values());
        assertNeedsIO(adapter.handleResult(result), SuitSelectionIOEvent.class);
    }

    @Test
    void handleResult_biddingCompleted_returnsNeedsIO() {
        assertNeedsIO(adapter.handleResult(new BiddingCompleted()), BiddingCompletedIOEvent.class);
    }

    @Test
    void handleResult_endOfTurnResult_returnsNeedsIO() {
        EndOfTurnResult result = new EndOfTurnResult("Player1", TEST_CARD);
        assertNeedsIO(adapter.handleResult(result), EndOfTurnIOEvent.class);
    }

    @Test
    void handleResult_endOfTrickResult_returnsNeedsIO() {
        EndOfTrickResult result = new EndOfTrickResult("Player1", TEST_CARD, "Player2");
        assertNeedsIO(adapter.handleResult(result), EndOfTrickIOEvent.class);
    }

    @Test
    void handleResult_endOfRoundResult_returnsNeedsIO() {
        EndOfRoundResult result = new EndOfRoundResult("Player1", TEST_CARD);
        assertNeedsIO(adapter.handleResult(result), EndOfRoundIOEvent.class);
    }

    // =========================================================================
    // handleResponse — null input → ContinueCommand
    // =========================================================================

    @Test
    void handleResponse_nullInput_returnsContinueCommand() {
        PlayCardResult result = playCardResult(humanPlayer);

        AdapterResponse response = adapter.handleResponse(new Response(null), result);

        assertThat(response.command()).isInstanceOf(ContinueCommand.class);
    }

    // =========================================================================
    // handleResponse — BidTurnResult
    // =========================================================================

    @Test
    void handleResponse_bidTurn_validChoice_returnsBidCommand() {
        BidTurnResult result = new BidTurnResult(
                "Player1", Suit.HEARTS, null,
                List.of(BidType.PASS, BidType.SOLO), List.of(), humanPlayer
        );

        AdapterResponse response = adapter.handleResponse(new Response("2"), result);

        assertThat(response.command()).isInstanceOf(BidCommand.class);
        assertThat(((BidCommand) response.command()).bid()).isEqualTo(BidType.SOLO);
    }

    @Test
    void handleResponse_bidTurn_nonNumericInput_returnsUiOnlyError() {
        BidTurnResult result = bidTurnResult(humanPlayer);

        AdapterResponse response = adapter.handleResponse(new Response("abc"), result);

        assertUiOnlyError(response);
    }

    @Test
    void handleResponse_bidTurn_outOfRangeChoice_returnsUiOnlyError() {
        BidTurnResult result = new BidTurnResult(
                "Player1", Suit.HEARTS, null,
                List.of(BidType.PASS), List.of(), humanPlayer
        );

        AdapterResponse response = adapter.handleResponse(new Response("99"), result);

        assertUiOnlyError(response);
    }

    // =========================================================================
    // handleResponse — SuitSelectionRequired
    // =========================================================================

    @Test
    void handleResponse_suitSelection_validChoice_returnsSuitCommand() {
        SuitSelectionRequired result = new SuitSelectionRequired("Player1", BidType.SOLO, Suit.values());

        AdapterResponse response = adapter.handleResponse(new Response("1"), result);

        assertThat(response.command()).isInstanceOf(SuitCommand.class);
        assertThat(((SuitCommand) response.command()).suit()).isEqualTo(Suit.values()[0]);
    }

    @Test
    void handleResponse_suitSelection_nonNumericInput_returnsUiOnlyError() {
        SuitSelectionRequired result = new SuitSelectionRequired("Player1", BidType.SOLO, Suit.values());

        AdapterResponse response = adapter.handleResponse(new Response("hearts"), result);

        assertUiOnlyError(response);
    }

    // =========================================================================
    // handleResponse — PlayCardResult (human)
    // =========================================================================

    @Test
    void handleResponse_playCard_validCardChoice_returnsCardCommand() {
        Card card = new Card(Suit.CLUBS, Rank.TEN);
        PlayCardResult result = new PlayCardResult(
                List.of(), false, List.of(), List.of(),
                1, humanPlayer, List.of(card), null
        );

        AdapterResponse response = adapter.handleResponse(new Response("1"), result);

        assertThat(response.command()).isInstanceOf(CardCommand.class);
        assertThat(((CardCommand) response.command()).card()).isEqualTo(card);
    }

    @Test
    void handleResponse_playCard_zeroWithNoPreviousTrick_returnsUiOnlyMessage() {
        PlayCardResult result = new PlayCardResult(
                List.of(), false, List.of(), List.of(),
                1, humanPlayer, List.of(TEST_CARD), null   // null = no last trick
        );

        AdapterResponse response = adapter.handleResponse(new Response("0"), result);

        assertThat(response.command()).isNull();
        assertThat(response.immediateEvents()).isNotEmpty();
    }


    @Test
    void handleResponse_playCard_outOfRangeChoice_returnsUiOnlyError() {
        PlayCardResult result = new PlayCardResult(
                List.of(), false, List.of(), List.of(),
                1, humanPlayer, List.of(TEST_CARD), null
        );

        AdapterResponse response = adapter.handleResponse(new Response("99"), result);

        assertThat(response.command()).isNull();
    }

    @Test
    void handleResponse_playCard_nonNumericInput_returnsUiOnlyError() {
        PlayCardResult result = playCardResult(humanPlayer);

        AdapterResponse response = adapter.handleResponse(new Response("ace of spades"), result);

        assertUiOnlyError(response);
    }
    // Replace assertNeedsIO helper
    private void assertNeedsIO(AdapterResult result, Class<? extends IOEvent> eventClass) {
        AdapterResult.NeedsIO needsIO = assertInstanceOf(AdapterResult.NeedsIO.class, result);
        assertThat(needsIO.event()).isInstanceOf(eventClass);
    }

    // Replace assertUiOnlyError helper
    private void assertUiOnlyError(AdapterResponse response) {
        assertThat(response.command()).isNull();
        assertThat(response.shouldReRenderLastResult()).isTrue();
        MessageIOEvent message = assertInstanceOf(
                MessageIOEvent.class, response.immediateEvents().getFirst()
        );
        assertThat(message.text()).contains("Please try again");
    }
    // =========================================================================
    // handleResponse — ProposalRejected
    // =========================================================================

    @Test
    void handleResponse_proposalRejected_choiceOne_returnsPass() {
        ProposalRejected result = new ProposalRejected("Player1");

        AdapterResponse response = adapter.handleResponse(new Response("1"), result);

        assertThat(((BidCommand) response.command()).bid()).isEqualTo(BidType.PASS);
    }

    @Test
    void handleResponse_proposalRejected_choiceTwo_returnsSoloProposal() {
        ProposalRejected result = new ProposalRejected("Player1");

        AdapterResponse response = adapter.handleResponse(new Response("2"), result);

        assertThat(((BidCommand) response.command()).bid()).isEqualTo(BidType.SOLO_PROPOSAL);
    }

    // =========================================================================
    // handleResponse — BiddingCompleted (press enter)
    // =========================================================================

    @Test
    void handleResponse_biddingCompleted_returnsContinueCommand() {
        AdapterResponse response = adapter.handleResponse(new Response(""), new BiddingCompleted());

        assertThat(response.command()).isInstanceOf(ContinueCommand.class);
    }

    // =========================================================================
    // handleResponse — End of turn/trick/round (press enter)
    // =========================================================================

    @Test
    void handleResponse_endOfTurn_returnsContinueCommand() {
        EndOfTurnResult result = new EndOfTurnResult("Player1", TEST_CARD);

        AdapterResponse response = adapter.handleResponse(new Response(""), result);

        assertThat(response.command()).isInstanceOf(ContinueCommand.class);
    }

    @Test
    void handleResponse_endOfTrick_returnsContinueCommand() {
        EndOfTrickResult result = new EndOfTrickResult("Player1", TEST_CARD, "Player2");

        AdapterResponse response = adapter.handleResponse(new Response(""), result);

        assertThat(response.command()).isInstanceOf(ContinueCommand.class);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PlayCardResult playCardResult(Player player) {
        return new PlayCardResult(
                List.of(), false, List.of(), List.of(),
                1, player, List.of(TEST_CARD), null
        );
    }

    private BidTurnResult bidTurnResult(Player player) {
        return new BidTurnResult(
                player.equals(humanPlayer) ? "Human" : "Bot",
                Suit.HEARTS, null,
                List.of(BidType.PASS, BidType.SOLO),
                List.of(), player
        );
    }

}