package cli.adapter;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.results.*;
import cli.elements.Response;
import cli.events.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("CLI Adapter")
class AdapterTest {

    private WhistGame game;
    private Player humanPlayer;
    private Player botPlayer;
    private PlayerId humanId;
    private PlayerId botId;
    private Adapter adapter;

    private static final Card TEST_CARD = new Card(Suit.HEARTS, Rank.ACE);

    @BeforeEach
    void setUp() {
        // Arrange
        game = mock(WhistGame.class);
        humanPlayer = mock(Player.class);
        botPlayer = mock(Player.class);

        humanId = new PlayerId("human-123");
        botId = new PlayerId("bot-456");

        // Human Mock Setup
        lenient().when(humanPlayer.getId()).thenReturn(humanId);
        lenient().when(humanPlayer.getName()).thenReturn("Alice");
        lenient().when(humanPlayer.getRequiresConfirmation()).thenReturn(true);

        // Bot Mock Setup
        lenient().when(botPlayer.getId()).thenReturn(botId);
        lenient().when(botPlayer.getName()).thenReturn("Bob-Bot");
        lenient().when(botPlayer.getRequiresConfirmation()).thenReturn(false);

        lenient().when(game.getPlayers()).thenReturn(List.of(humanPlayer, botPlayer));

        adapter = new Adapter(game);
    }

    @Nested
    @DisplayName("handleResult() - Domain to View Translation")
    class HandleResultTests {

        @Test
        @DisplayName("PlayCardResult for Bot returns Immediate CardCommand")
        void playCard_BotPlayer_ReturnsImmediateCommand() {
            when(botPlayer.chooseCard(any())).thenReturn(TEST_CARD);
            PlayCardResult result = playCardResult(botPlayer);

            AdapterResult adapterResult = adapter.handleResult(result);

            assertThat(adapterResult).isInstanceOf(AdapterResult.Immediate.class);
            GameCommand command = ((AdapterResult.Immediate) adapterResult).command();

            assertThat(command).isInstanceOf(CardCommand.class);
            assertThat(((CardCommand) command).card()).isEqualTo(TEST_CARD);
            verify(botPlayer).chooseCard(any());
        }

        @Test
        @DisplayName("PlayCardResult for Human returns NeedsIO with Confirmation preamble")
        void playCard_HumanPlayer_ReturnsNeedsIO() {
            PlayCardResult result = playCardResult(humanPlayer);

            AdapterResult adapterResult = adapter.handleResult(result);

            assertThat(adapterResult).isInstanceOf(AdapterResult.NeedsIO.class);
            AdapterResult.NeedsIO needsIO = (AdapterResult.NeedsIO) adapterResult;

            // FIXED: Using preamble() instead of preambles()
            assertThat(needsIO.preamble()).hasSize(1);
            assertThat(needsIO.preamble().getFirst()).isInstanceOf(PlayEvents.ConfirmationIOEvent.class);
            assertThat(needsIO.event()).isInstanceOf(PlayEvents.PlayCardIOEvent.class);
        }

        @Test
        @DisplayName("BidTurnResult for Bot returns Immediate BidCommand")
        void bidTurn_BotPlayer_ReturnsImmediateCommand() {
            Bid mockBid = mock(Bid.class);
            when(mockBid.getType()).thenReturn(BidType.SOLO);
            when(mockBid.determineTrump(any())).thenReturn(Suit.HEARTS);
            when(botPlayer.chooseBid()).thenReturn(mockBid);

            BidTurnResult result = bidTurnResult(botPlayer);

            AdapterResult adapterResult = adapter.handleResult(result);

            assertThat(adapterResult).isInstanceOf(AdapterResult.Immediate.class);
            GameCommand command = ((AdapterResult.Immediate) adapterResult).command();

            assertThat(command).isInstanceOf(BidCommand.class);
            assertThat(((BidCommand) command).bid()).isEqualTo(BidType.SOLO);
            assertThat(((BidCommand) command).suit()).isEqualTo(Suit.HEARTS);
        }

        @Test
        @DisplayName("Flow Events (EndOfTrick, ScoreBoard) translate to NeedsIO")
        void flowEvents_ReturnNeedsIO() {
            AdapterResult result = adapter.handleResult(new ScoreBoardResult(List.of(), List.of()));
            assertThat(result).isInstanceOf(AdapterResult.NeedsIO.class);
            assertThat(((AdapterResult.NeedsIO) result).event()).isInstanceOf(CountEvents.ScoreBoardIOEvent.class);
        }
    }

    @Nested
    @DisplayName("handleResponse() - View to Domain Translation")
    class HandleResponseTests {

        @Test
        @DisplayName("Empty or blank input returns null Domain Command (skip)")
        void blankInput_ReturnsNullCommand() {
            AdapterResponse response = adapter.handleResponse(new Response("   "), playCardResult(humanPlayer));

            assertThat(response.command()).isNull();
            assertThat(response.shouldReRenderLastResult()).isFalse();
        }

        @Test
        @DisplayName("PlayCardResult: Valid selection maps correctly to CardCommand")
        void playCard_ValidInput_ReturnsCardCommand() {
            PlayCardResult result = playCardResult(humanPlayer);
            AdapterResponse response = adapter.handleResponse(new Response("1"), result);

            assertThat(response.command()).isInstanceOf(CardCommand.class);
            assertThat(((CardCommand) response.command()).card()).isEqualTo(TEST_CARD);
        }

        @Test
        @DisplayName("PlayCardResult: Input '0' without history returns UI Error")
        void playCard_ZeroInputNoHistory_ReturnsUIError() {
            PlayCardResult result = playCardResult(humanPlayer); // lastPlayedTrick is null by default
            AdapterResponse response = adapter.handleResponse(new Response("0"), result);

            assertUiOnlyError(response, "No tricks have been played yet!");
        }

        @ParameterizedTest(name = "PlayCardResult: Out of bounds input ({0}) returns UI Error")
        @ValueSource(strings = {"-1", "99"})
        void playCard_OutOfBoundsInput_ReturnsUIError(String invalidInput) {
            PlayCardResult result = playCardResult(humanPlayer);
            AdapterResponse response = adapter.handleResponse(new Response(invalidInput), result);

            assertUiOnlyError(response, "Invalid selection");
        }

        @Test
        @DisplayName("ParticipatingPlayersResult: Valid indices map to PlayerIds successfully")
        void participatingPlayers_ValidInput_MapsToPlayerIds() {
            // FIXED: Added 'true' to satisfy the multiSelect boolean requirement
            ParticipatingPlayersResult result = new ParticipatingPlayersResult(List.of("Alice", "Bob-Bot"), true);

            // User types "1" to select Alice
            AdapterResponse response = adapter.handleResponse(new Response("1"), result);

            assertThat(response.command()).isInstanceOf(PlayerListCommand.class);

            // FIXED: Using playerIds() instead of players()
            List<PlayerId> ids = ((PlayerListCommand) response.command()).playerIds();

            assertThat(ids).hasSize(1);
            assertThat(ids.getFirst()).isEqualTo(humanId);
        }

        @Test
        @DisplayName("Invalid String format gracefully catches Exception and returns UI Error")
        void unparseableInput_CatchesException_ReturnsUIError() {
            PlayCardResult result = playCardResult(humanPlayer);

            // User types letters instead of numbers
            AdapterResponse response = adapter.handleResponse(new Response("ABC"), result);

            assertUiOnlyError(response, "Invalid input: \"ABC\". Please try again.");
        }
    }

    // =========================================================================
    // Factory Helpers
    // =========================================================================

    private void assertUiOnlyError(AdapterResponse response, String expectedMessagePart) {
        assertThat(response.command()).isNull();
        assertThat(response.shouldReRenderLastResult()).isTrue();

        MessageIOEvent message = (MessageIOEvent) response.immediateEvents().getFirst();
        assertThat(message.text()).contains(expectedMessagePart);
    }

    private PlayCardResult playCardResult(Player player) {
        return new PlayCardResult(
                List.of(), false, List.of(), List.of(),
                1, player, List.of(TEST_CARD), null
        );
    }

    private BidTurnResult bidTurnResult(Player player) {
        return new BidTurnResult(
                player.getName(), Suit.HEARTS, null,
                List.of(BidType.PASS, BidType.SOLO),
                List.of(), player
        );
    }
}