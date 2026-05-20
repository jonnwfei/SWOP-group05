package base.domain.round;

import base.domain.WhistRules;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.MiserieBid;
import base.domain.bid.PassBid;
import base.domain.card.Suit;
import base.domain.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RoundRestorationService")
class RoundRestorationServiceTest {

    @Mock private Round round;
    @Mock private Player p1;
    @Mock private Player p2;
    @Mock private Player p3;
    @Mock private Player p4;

    private AutoCloseable mocks;
    private RoundRestorationService service;

    // Bid is a sealed interface; all permits are records — use a real instance.
    private static final Bid A_BID = new PassBid();
    private static final Bid B_BID = new MiserieBid(BidType.MISERIE);
    private List<Player> fourPlayers;
    private List<Integer> fourDeltas;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new RoundRestorationService();
        fourPlayers = List.of(p1, p2, p3, p4);
        fourDeltas = List.of(75, -25, -25, -25);

        lenient().when(round.getPlayers()).thenReturn(fourPlayers);
    }

    @AfterEach
    void tearDown() throws Exception { mocks.close(); }

    // =========================================================================
    // validate — null / missing highest bid
    // =========================================================================

    @Nested
    @DisplayName("validate — null highest bid")
    class NullHighestBidTests {

        @Test
        @DisplayName("Throws IllegalArgumentException when highestBid is null")
        void throwsOnNullBid() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.restore(round, null, Suit.HEARTS,
                            List.of(p1), 8, List.of(), fourDeltas));
        }
    }

    // =========================================================================
    // validate — participants
    // =========================================================================

    @Nested
    @DisplayName("validate — participants")
    class ParticipantsTests {

        @Test
        @DisplayName("Throws when participants list is null")
        void throwsOnNullParticipants() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            null, 8, List.of(), fourDeltas));
        }

        @Test
        @DisplayName("Throws when participants list is empty")
        void throwsOnEmptyParticipants() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.restore(round, B_BID, Suit.HEARTS, // bij een pas mag de part list leeg zijn -> miserie nodig
                            List.of(), 8, List.of(), fourDeltas));
        }

        @Test
        @DisplayName("Throws when participants list contains null element")
        void throwsOnNullElement() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            Arrays.asList(p1, null), 8, List.of(), fourDeltas));
        }
    }

    // =========================================================================
    // validate — tricksWon
    // =========================================================================

    @Nested
    @DisplayName("validate — tricksWon range")
    class TricksWonTests {

        @Test
        @DisplayName("Throws when tricksWon is below -1")
        void throwsWhenBelowMinus1() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            List.of(p1), -2, List.of(), fourDeltas));
        }

        @Test
        @DisplayName("Throws when tricksWon exceeds MAX_TRICKS (13)")
        void throwsWhenAboveMax() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            List.of(p1), 14, List.of(), fourDeltas));
        }

        @Test
        @DisplayName("Accepts -1 (sentinel for 'use real tricks')")
        void acceptsMinus1() {
            assertDoesNotThrow(() ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            List.of(p1), -1, List.of(), fourDeltas));
        }

        @Test
        @DisplayName("Accepts 0 tricks won")
        void accepts0() {
            assertDoesNotThrow(() ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            List.of(p1), 0, List.of(), fourDeltas));
        }

        @Test
        @DisplayName("Accepts exactly MAX_TRICKS (13)")
        void acceptsMaxTricks() {
            assertDoesNotThrow(() ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            List.of(p1), WhistRules.MAX_TRICKS, List.of(), fourDeltas));
        }
    }

    // =========================================================================
    // validate — score deltas
    // =========================================================================

    @Nested
    @DisplayName("validate — restoredScoreDeltas")
    class ScoreDeltasTests {

        @Test
        @DisplayName("Throws when restoredScoreDeltas is null")
        void throwsOnNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            List.of(p1), 8, List.of(), null));
        }

        @Test
        @DisplayName("Throws when delta count differs from player count")
        void throwsOnSizeMismatch() {
            // round has 4 players but we pass 3 deltas
            assertThrows(IllegalArgumentException.class, () ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            List.of(p1), 8, List.of(), List.of(10, -10, -10)));
        }
    }

    // =========================================================================
    // validate — miserie winners
    // =========================================================================

    @Nested
    @DisplayName("validate — miserieWinners")
    class MiserieWinnersTests {

        @Test
        @DisplayName("Throws when miserie winner is not a participant")
        void throwsOnNonParticipant() {
            // p2 is not in participants list (only p1 is)
            assertThrows(IllegalArgumentException.class, () ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            List.of(p1), 8, List.of(p2), fourDeltas));
        }

        @Test
        @DisplayName("Accepts null miserieWinners without throwing")
        void acceptsNullMiserieWinners() {
            assertDoesNotThrow(() ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            List.of(p1), 8, null, fourDeltas));
        }

        @Test
        @DisplayName("Accepts empty miserieWinners list")
        void acceptsEmptyMiserieWinners() {
            assertDoesNotThrow(() ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            List.of(p1), 8, List.of(), fourDeltas));
        }

        @Test
        @DisplayName("Accepts miserieWinners that are all participants")
        void acceptsValidMiserieWinners() {
            assertDoesNotThrow(() ->
                    service.restore(round, A_BID, Suit.HEARTS,
                            List.of(p1, p2), 0, List.of(p1), fourDeltas));
        }
    }

    // =========================================================================
    // restore — successful delegation
    // =========================================================================

    @Nested
    @DisplayName("restore — successful delegation to round.restoreState")
    class SuccessfulRestoreTests {

        @Test
        @DisplayName("Calls round.restoreState with all provided arguments on valid input")
        void delegatesToRestoreState() {
            List<Player> participants = List.of(p1);
            List<Player> miserieWinners = List.of();

            service.restore(round, A_BID, Suit.SPADES,
                    participants, 8, miserieWinners, fourDeltas);

            verify(round).restoreState(
                    A_BID, Suit.SPADES, participants,
                    8, miserieWinners, fourDeltas);
        }

        @Test
        @DisplayName("Calls restoreState with null trumpSuit (allowed)")
        void delegatesWithNullTrump() {
            service.restore(round, A_BID, null,
                    List.of(p1), -1, null, fourDeltas);

            verify(round).restoreState(
                    A_BID, null, List.of(p1),
                    -1, null, fourDeltas);
        }

        @Test
        @DisplayName("Does not throw for maximum valid tricksWon")
        void maxTricksWonValid() {
            service.restore(round, A_BID, Suit.CLUBS,
                    List.of(p1), 13, List.of(), fourDeltas);

            verify(round).restoreState(
                    eq(A_BID), eq(Suit.CLUBS), eq(List.of(p1)),
                    eq(13), eq(List.of()), eq(fourDeltas));
        }
    }
}
