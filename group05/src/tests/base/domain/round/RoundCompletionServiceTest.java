package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidManager;
import base.domain.bid.BidType;
import base.domain.bid.MiserieBid;
import base.domain.bid.PassBid;
import base.domain.bid.SoloBid;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.trick.Trick;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RoundCompletionService")
class RoundCompletionServiceTest {

    @Mock private Round round;
    @Mock private BidManager bidManager;

    private AutoCloseable mocks;
    private RoundCompletionService service;

    private final PlayerId id1 = new PlayerId();
    private final PlayerId id2 = new PlayerId();

    /** Real bid instances — Bid is a sealed interface whose permits are all records. */
    private static final Bid SOLO_BID    = new SoloBid(BidType.SOLO, Suit.HEARTS);
    private static final Bid PASS_BID    = new PassBid();
    private static final Bid MISERIE_BID = new MiserieBid(BidType.MISERIE);

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new RoundCompletionService();

        lenient().when(round.getBidManager()).thenReturn(bidManager);
        lenient().when(round.getHighestBid()).thenReturn(SOLO_BID);
        lenient().when(round.getPlayers()).thenReturn(List.of(
                mock(Player.class), mock(Player.class),
                mock(Player.class), mock(Player.class)));
    }

    @AfterEach
    void tearDown() throws Exception { mocks.close(); }

    // =========================================================================
    // isFinished — already marked
    // =========================================================================

    @Nested
    @DisplayName("isFinished — already marked finished")
    class AlreadyMarkedTests {

        @Test
        @DisplayName("Returns true immediately when round is already marked finished")
        void returnsTrueWhenMarked() {
            when(round.isMarkedFinished()).thenReturn(true);
            assertTrue(service.isFinished(round));
        }

        @Test
        @DisplayName("Does not short-circuit when not yet marked finished")
        void checksAutoFinishWhenNotMarked() {
            when(round.isMarkedFinished()).thenReturn(false);
            when(round.getTricks()).thenReturn(List.of()); // 0 tricks — not finished
            when(round.getHighestBid()).thenReturn(SOLO_BID); // SOLO, not MISERIE
            when(round.getBids()).thenReturn(List.of());
            assertFalse(service.isFinished(round));
        }
    }

    // =========================================================================
    // shouldAutoFinish — max tricks
    // =========================================================================

    @Nested
    @DisplayName("shouldAutoFinish — max tricks reached")
    class MaxTricksTests {

        @Test
        @DisplayName("Returns true when 13 tricks have been played")
        void trueAt13Tricks() {
            when(round.isMarkedFinished()).thenReturn(false);
            List<Trick> tricks = java.util.Collections.nCopies(13, mock(Trick.class));
            when(round.getTricks()).thenReturn(tricks);

            assertTrue(service.shouldAutoFinish(round));
            assertTrue(service.isFinished(round));
        }

        @Test
        @DisplayName("Returns false with fewer than 13 tricks for non-miserie bid")
        void falseBeforeMax() {
            when(round.isMarkedFinished()).thenReturn(false);
            when(round.getTricks()).thenReturn(List.of(mock(Trick.class))); // 1 trick
            when(round.getHighestBid()).thenReturn(SOLO_BID);  // SOLO, not MISERIE

            assertFalse(service.shouldAutoFinish(round));
        }
    }

    // =========================================================================
    // isFinished — all-pass
    // =========================================================================

    @Nested
    @DisplayName("isFinished — all-pass round")
    class AllPassTests {

        @BeforeEach
        void setPassBid() {
            when(round.getHighestBid()).thenReturn(PASS_BID);
        }

        @Test
        @DisplayName("Finished when all 4 players passed")
        void finishedWhenAllPassed() {
            when(round.isMarkedFinished()).thenReturn(false);
            when(round.getTricks()).thenReturn(List.of());
            // Bid is sealed; use real PassBid instances instead of mock(Bid.class)
            when(round.getBids()).thenReturn(List.of(
                    new PassBid(), new PassBid(), new PassBid(), new PassBid()));

            assertTrue(service.isFinished(round));
        }

        @Test
        @DisplayName("Not finished when only 3 of 4 players passed")
        void notFinishedWhenPartialPass() {
            when(round.isMarkedFinished()).thenReturn(false);
            when(round.getTricks()).thenReturn(List.of());
            when(round.getBids()).thenReturn(List.of(
                    new PassBid(), new PassBid(), new PassBid()));

            assertFalse(service.isFinished(round));
        }
    }

    // =========================================================================
    // shouldAutoFinish — Miserie early termination
    // =========================================================================

    @Nested
    @DisplayName("shouldAutoFinish — Miserie early termination")
    class MiserieTerminationTests {

        private Trick trickWonBy(PlayerId winner) {
            Trick trick = mock(Trick.class);
            when(trick.getWinningPlayerId()).thenReturn(winner);
            return trick;
        }


        @Test
        @DisplayName("Does NOT terminate early when one miserie bidder has not taken a trick")
        void doesNotTerminateIfSomeoneStillSafe() {
            when(round.isMarkedFinished()).thenReturn(false);
            when(round.getHighestBid()).thenReturn(MISERIE_BID);

            when(bidManager.findMiserieParticipants(BidType.MISERIE))
                    .thenReturn(List.of(id1, id2));
            // Build the trick first — nesting when().thenReturn() inside another
            // thenReturn() triggers Mockito's UnfinishedStubbingException.
            Trick trick = trickWonBy(id1); // id1 won, id2 has not → not yet all failed
            when(round.getTricks()).thenReturn(List.of(trick));

            assertFalse(service.shouldAutoFinish(round));
        }

        @Test
        @DisplayName("Does NOT terminate when no miserie bidders exist (empty list)")
        void doesNotTerminateWithEmptyParticipants() {
            when(round.isMarkedFinished()).thenReturn(false);
            when(round.getHighestBid()).thenReturn(MISERIE_BID);
            when(bidManager.findMiserieParticipants(BidType.MISERIE))
                    .thenReturn(List.of());
            when(round.getTricks()).thenReturn(List.of());

            assertFalse(service.shouldAutoFinish(round));
        }
    }
}
