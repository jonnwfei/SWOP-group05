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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the CLI Adapter.
 * The adapter acts as a bridge between the Domain State Machine and the In-Terminal View.
 */
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
        game = mock(WhistGame.class);
        humanPlayer = mock(Player.class);
        botPlayer = mock(Player.class);

        humanId = new PlayerId("human-123");
        botId = new PlayerId("bot-456");

        lenient().when(humanPlayer.getId()).thenReturn(humanId);
        lenient().when(humanPlayer.getName()).thenReturn("Alice");
        lenient().when(humanPlayer.getRequiresConfirmation()).thenReturn(true);

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

            assertTrue(adapterResult instanceof AdapterResult.Immediate);
            GameCommand command = ((AdapterResult.Immediate) adapterResult).command();

            assertTrue(command instanceof CardCommand);
            assertEquals(TEST_CARD, ((CardCommand) command).card());
            verify(botPlayer).chooseCard(any());
        }

        @Test
        @DisplayName("PlayCardResult for Human returns NeedsIO with Confirmation preamble")
        void playCard_HumanPlayer_ReturnsNeedsIO() {
            PlayCardResult result = playCardResult(humanPlayer);

            AdapterResult adapterResult = adapter.handleResult(result);

            assertTrue(adapterResult instanceof AdapterResult.NeedsIO);
            AdapterResult.NeedsIO needsIO = (AdapterResult.NeedsIO) adapterResult;

            assertEquals(1, needsIO.preamble().size());
            assertTrue(needsIO.preamble().get(0) instanceof PlayEvents.ConfirmationIOEvent);
            assertTrue(needsIO.event() instanceof PlayEvents.PlayCardIOEvent);
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

            assertTrue(adapterResult instanceof AdapterResult.Immediate);
            GameCommand command = ((AdapterResult.Immediate) adapterResult).command();

            assertTrue(command instanceof BidCommand);
            assertEquals(BidType.SOLO, ((BidCommand) command).bid());
            assertEquals(Suit.HEARTS, ((BidCommand) command).suit());
        }

        @Test
        @DisplayName("Flow Events (EndOfTrick, ScoreBoard) translate to NeedsIO")
        void flowEvents_ReturnNeedsIO() {
            AdapterResult result = adapter.handleResult(new ScoreBoardResult(List.of(), List.of()));

            assertTrue(result instanceof AdapterResult.NeedsIO);
            assertTrue(((AdapterResult.NeedsIO) result).event() instanceof CountEvents.ScoreBoardIOEvent);
        }
    }

    @Nested
    @DisplayName("handleResponse() - View to Domain Translation")
    class HandleResponseTests {

        @Test
        @DisplayName("Empty or blank input returns null Domain Command (skip)")
        void blankInput_ReturnsNullCommand() {
            AdapterResponse response = adapter.handleResponse(new Response("   "), playCardResult(humanPlayer));

            assertNull(response.command());
            assertFalse(response.shouldReRenderLastResult());
        }

        @Test
        @DisplayName("PlayCardResult: Valid selection maps correctly to CardCommand")
        void playCard_ValidInput_ReturnsCardCommand() {
            PlayCardResult result = playCardResult(humanPlayer);
            AdapterResponse response = adapter.handleResponse(new Response("1"), result);

            assertTrue(response.command() instanceof CardCommand);
            assertEquals(TEST_CARD, ((CardCommand) response.command()).card());
        }

        @Test
        @DisplayName("PlayCardResult: Input '0' without history returns UI Error")
        void playCard_ZeroInputNoHistory_ReturnsUIError() {
            PlayCardResult result = playCardResult(humanPlayer);
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
            ParticipatingPlayersResult result = new ParticipatingPlayersResult(List.of("Alice", "Bob-Bot"), true);

            AdapterResponse response = adapter.handleResponse(new Response("1"), result);

            assertTrue(response.command() instanceof PlayerListCommand);
            List<PlayerId> ids = ((PlayerListCommand) response.command()).playerIds();

            assertEquals(1, ids.size());
            assertEquals(humanId, ids.get(0));
        }

        @Test
        @DisplayName("Invalid String format gracefully catches Exception and returns UI Error")
        void unparseableInput_CatchesException_ReturnsUIError() {
            PlayCardResult result = playCardResult(humanPlayer);

            AdapterResponse response = adapter.handleResponse(new Response("ABC"), result);

            assertUiOnlyError(response, "Invalid input: \"ABC\". Please try again.");
        }
    }

    // =========================================================================
    // Factory Helpers
    // =========================================================================

    private void assertUiOnlyError(AdapterResponse response, String expectedMessagePart) {
        assertNull(response.command(), "An error response should not yield a domain command.");
        assertTrue(response.shouldReRenderLastResult(), "UI errors should trigger a re-render of the prompt.");

        MessageIOEvent message = (MessageIOEvent) response.immediateEvents().get(0);
        assertTrue(message.text().contains(expectedMessagePart));
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