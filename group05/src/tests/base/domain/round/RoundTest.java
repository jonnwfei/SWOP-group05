package base.domain.round;

import base.domain.bid.*;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.scores.ScoringRegistry;
import base.domain.trick.Trick;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Round Domain Entity Tests")
class RoundTest {

    @Mock private Player p1;
    @Mock private Player p2;
    @Mock private Player p3;
    @Mock private Player p4;
    @Mock private Player externalPlayer;

    private AutoCloseable mocks;
    private List<Player> players;
    private Round round;
    private ScoringRegistry registry;

    private final PlayerId id1 = new PlayerId();
    private final PlayerId id2 = new PlayerId();
    private final PlayerId id3 = new PlayerId();
    private final PlayerId id4 = new PlayerId();

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        lenient().when(p1.getId()).thenReturn(id1);
        lenient().when(p2.getId()).thenReturn(id2);
        lenient().when(p3.getId()).thenReturn(id3);
        lenient().when(p4.getId()).thenReturn(id4);
        lenient().when(externalPlayer.getId()).thenReturn(new PlayerId());

        lenient().when(p1.getHand()).thenReturn(createDummyHand(13));
        lenient().when(p2.getHand()).thenReturn(createDummyHand(13));
        lenient().when(p3.getHand()).thenReturn(createDummyHand(13));
        lenient().when(p4.getHand()).thenReturn(createDummyHand(13));

        players = List.of(p1, p2, p3, p4);
        round = new Round(players, p1, 2);
        registry = new ScoringRegistry();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Nested
    @DisplayName("Constructor & Initialization Guards")
    class ConstructorTests {

        @Test
        @DisplayName("Successfully initializes with 4 valid players")
        void shouldInitializeSuccessfully() {
            assertEquals(4, round.getPlayers().size());
            assertEquals(p1, round.getCurrentPlayer());
            assertEquals(2, round.getMultiplier());
            assertFalse(round.isFinished());
            assertEquals(List.of(0, 0, 0, 0), round.getScoreDeltas());
            assertNotNull(round.getBidManager());
            assertNull(round.getRoundContract());
        }

        @Test
        @DisplayName("Rejects null or incorrectly sized player lists")
        void shouldRejectInvalidPlayerLists() {
            assertThrows(IllegalArgumentException.class, () -> new Round(null, p1, 1));
            assertThrows(IllegalArgumentException.class, () -> new Round(List.of(p1, p2, p3), p1, 1));
            assertThrows(IllegalArgumentException.class, () -> new Round(List.of(p1, p2, p3, p4, externalPlayer), p1, 1));
        }

        @Test
        @DisplayName("Rejects player lists containing null elements")
        void shouldRejectListsWithNullElements() {
            List<Player> playersWithNull = Arrays.asList(p1, p2, null, p4);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Round(playersWithNull, p1, 1));
            assertTrue(ex.getMessage().contains("non-null players"));
        }

        @Test
        @DisplayName("Rejects invalid starting players")
        void shouldRejectInvalidStartingPlayers() {
            assertThrows(IllegalArgumentException.class, () -> new Round(players, null, 1));
            assertThrows(IllegalArgumentException.class, () -> new Round(players, externalPlayer, 1));
        }

        @Test
        @DisplayName("Rejects zero or negative multipliers")
        void shouldRejectInvalidMultiplier() {
            assertThrows(IllegalArgumentException.class, () -> new Round(players, p1, 0));
            assertThrows(IllegalArgumentException.class, () -> new Round(players, p1, -1));
        }
    }

    @Nested
    @DisplayName("State Machine Transitions (Play Phase)")
    class StateMachineTests {

        @Test
        @DisplayName("startPlayPhase enforces non-null parameters and throws if already finished")
        void startPlayPhase_Validation() {
            Bid highestBid = BidType.SOLO.instantiate(Suit.HEARTS);
            List<Bid> validBids = List.of(highestBid, new PassBid(), new PassBid(), new PassBid());

            assertThrows(NullPointerException.class, () -> round.startPlayPhase(validBids, null, Suit.HEARTS, p1));
            assertThrows(NullPointerException.class, () -> round.startPlayPhase(validBids, highestBid, Suit.HEARTS, null));
            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(validBids, highestBid, Suit.HEARTS, externalPlayer),
                    "First player must be in the round");

            // Force round finish to test state lock
            round.abortWithAllPass(createFourPassBids());
            assertThrows(IllegalStateException.class, () -> round.startPlayPhase(validBids, highestBid, Suit.HEARTS, p1));
        }

        @Test
        @DisplayName("abortWithAllPass throws if already finished or bids invalid")
        void abortWithAllPass_Validation() {
            assertThrows(IllegalArgumentException.class, () -> round.abortWithAllPass(null));
            assertThrows(IllegalArgumentException.class, () -> round.abortWithAllPass(List.of(new PassBid())));

            round.abortWithAllPass(createFourPassBids());
            assertThrows(IllegalStateException.class, () -> round.abortWithAllPass(createFourPassBids()));
        }

        @Test
        @DisplayName("advanceToNextPlayer throws if already finished")
        void advanceToNextPlayer_Validation() {
            round.abortWithAllPass(createFourPassBids());
            assertThrows(IllegalStateException.class, () -> round.advanceToNextPlayer());
        }

        @Test
        @DisplayName("finalizeTrick rejects nulls and throws if already finished")
        void finalizeTrick_Validation() {
            Trick mockTrick = mockCompletedTrick(id1);

            assertThrows(NullPointerException.class, () -> round.finalizeTrick(null, registry));
            assertThrows(NullPointerException.class, () -> round.finalizeTrick(mockTrick, null));

            round.abortWithAllPass(createFourPassBids());
            assertThrows(IllegalStateException.class, () -> round.finalizeTrick(mockTrick, registry));
        }
    }

    @Nested
    @DisplayName("Snapshot Restoration & Manual Count")
    class SnapshotTests {

        @Test
        @DisplayName("restoreFromSnapshot validation guards")
        void restoreValidation() {
            Bid someBid = new PassBid();
            List<PlayerId> parts = List.of(id1);
            List<Integer> deltas = List.of(0, 0, 0, 0);

            assertThrows(NullPointerException.class, () -> round.restoreFromSnapshot(null, Suit.HEARTS, parts, 13, null, deltas));
            assertThrows(NullPointerException.class, () -> round.restoreFromSnapshot(someBid, Suit.HEARTS, null, 13, null, deltas));
            assertThrows(NullPointerException.class, () -> round.restoreFromSnapshot(someBid, Suit.HEARTS, parts, 13, null, null));

            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(someBid, Suit.HEARTS, parts, 14, null, deltas));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(someBid, Suit.HEARTS, parts, 13, null, List.of(0)));

            round.abortWithAllPass(createFourPassBids());
            assertThrows(IllegalStateException.class, () -> round.restoreFromSnapshot(someBid, Suit.HEARTS, parts, 13, null, deltas));
        }

        @Test
        @DisplayName("resolveManualCount validation guards")
        void resolveManualCountValidation() {
            Bid someBid = new PassBid();
            List<PlayerId> parts = List.of(id1);

            assertThrows(NullPointerException.class, () -> round.resolveManualCount(null, Suit.HEARTS, parts, 10, null, registry));
            assertThrows(NullPointerException.class, () -> round.resolveManualCount(someBid, Suit.HEARTS, null, 10, null, registry));
            assertThrows(NullPointerException.class, () -> round.resolveManualCount(someBid, Suit.HEARTS, parts, 10, null, null));

            assertThrows(IllegalArgumentException.class, () -> round.resolveManualCount(someBid, Suit.HEARTS, parts, -2, null, registry));

            round.abortWithAllPass(createFourPassBids());
            assertThrows(IllegalStateException.class, () -> round.resolveManualCount(someBid, Suit.HEARTS, parts, 10, null, registry));
        }
    }

    // --- Helpers ---
    private List<Card> createDummyHand(int size) {
        List<Card> hand = new ArrayList<>();
        Suit[] suits = Suit.values();
        Rank[] ranks = Rank.values();
        for (int i = 0; i < size; i++) {
            hand.add(new Card(suits[i % suits.length], ranks[i % ranks.length]));
        }
        return hand;
    }

    private Trick mockCompletedTrick(PlayerId winnerId) {
        Trick mockTrick = mock(Trick.class);
        lenient().when(mockTrick.getTurns()).thenReturn(List.of(mock(), mock(), mock(), mock()));
        lenient().when(mockTrick.getWinningPlayerId()).thenReturn(winnerId);
        return mockTrick;
    }

    private List<Bid> createFourPassBids() {
        return List.of(new PassBid(), new PassBid(), new PassBid(), new PassBid());
    }
}