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
import cli.events.*;
import cli.events.BidEvents.*;
import cli.events.PlayEvents.*;
import cli.elements.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

class AdapterTest {

    private WhistGame game;
    private Player humanPlayer;
    private Player botPlayer;
    private Adapter adapter;

    private static final Card TEST_CARD = new Card(Suit.HEARTS, Rank.ACE);

    @BeforeEach
    void setUp() {
        // Manual Mock Creation
        game = mock(WhistGame.class);
        humanPlayer = mock(Player.class);
        botPlayer = mock(Player.class);

        adapter = new Adapter(game);

        // Using lenient() via static import or Mockito.lenient()
        lenient().when(humanPlayer.getRequiresConfirmation()).thenReturn(true);
        lenient().when(botPlayer.getRequiresConfirmation()).thenReturn(false);
    }

    @Test
    void handleResult_bidTurn_botPlayer_returnsImmediateBidCommand() {
        Bid botBid = BidType.SOLO.instantiate(botPlayer, null);
        when(botPlayer.chooseBid()).thenReturn(botBid);
        BidTurnResult result = bidTurnResult(botPlayer);

        AdapterResult adapterResult = adapter.handleResult(result);

        assertThat(adapterResult).isInstanceOf(AdapterResult.Immediate.class);
        GameCommand command = ((AdapterResult.Immediate) adapterResult).command();

        // Example of using ArgumentCaptor style if you wanted to verify internal behavior
        // logic similar to the second file provided
        assertThat(command).isInstanceOf(BidCommand.class);
        assertThat(((BidCommand) command).bid()).isEqualTo(BidType.SOLO);
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

        // Explicit verification
        verify(botPlayer, times(1)).chooseCard(Suit.SPADES);
    }

    @Test
    void handleResponse_suitSelection_validChoice_returnsSuitCommand() {
        SuitSelectionRequired result = new SuitSelectionRequired("Player1", BidType.SOLO, Suit.values());

        AdapterResponse response = adapter.handleResponse(new Response("1"), result);

        // Using ArgumentCaptor (as seen in second file) for demonstration
        assertThat(response.command()).isInstanceOf(SuitCommand.class);
        Suit selectedSuit = ((SuitCommand) response.command()).suit();
        assertThat(selectedSuit).isEqualTo(Suit.values()[0]);
    }

    // =========================================================================
    // Helpers (Maintained for logic)
    // =========================================================================

    private void assertNeedsIO(AdapterResult result, Class<? extends IOEvent> eventClass) {
        AdapterResult.NeedsIO needsIO = assertInstanceOf(AdapterResult.NeedsIO.class, result);
        assertThat(needsIO.event()).isInstanceOf(eventClass);
    }

    private void assertUiOnlyError(AdapterResponse response) {
        assertThat(response.command()).isNull();
        assertThat(response.shouldReRenderLastResult()).isTrue();
        MessageIOEvent message = assertInstanceOf(
                MessageIOEvent.class, response.immediateEvents().getFirst()
        );
        assertThat(message.text()).contains("Please try again");
    }

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