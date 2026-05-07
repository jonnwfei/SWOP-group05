package base.domain;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.PassBid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.commands.GameCommand;
import base.domain.deck.Deck;
import base.domain.observer.GameObserver;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.round.Round;
import base.domain.states.BidState;
import base.domain.states.CountState;
import base.domain.states.State;
import base.domain.states.StateStep;
import base.domain.strategy.HumanStrategy;
import base.domain.strategy.Strategy;
import base.domain.turn.BidTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Whist Game Aggregate Root")
class WhistGameTest {

    private WhistGame game;

    @Mock private Player mockPlayer1;
    @Mock private Player mockPlayer2;
    @Mock private Player mockPlayer3;
    @Mock private Player mockPlayer4;
    @Mock private Player mockPlayer5; // Used for rotation/queue tests

    private PlayerId id1, id2, id3, id4, id5;

    @BeforeEach
    void setUp() {
        game = new WhistGame();

        id1 = new PlayerId(); id2 = new PlayerId(); id3 = new PlayerId(); id4 = new PlayerId(); id5 = new PlayerId();

        setupMockPlayer(mockPlayer1, id1, "Alice");
        setupMockPlayer(mockPlayer2, id2, "Bob");
        setupMockPlayer(mockPlayer3, id3, "Charlie");
        setupMockPlayer(mockPlayer4, id4, "Diana");
        setupMockPlayer(mockPlayer5, id5, "Eve");
    }

    private void setupMockPlayer(Player player, PlayerId id, String name) {
        Strategy dummyStrategy = mock(HumanStrategy.class);
        lenient().when(player.getDecisionStrategy()).thenReturn(dummyStrategy);
        lenient().when(player.getId()).thenReturn(id);
        lenient().when(player.getName()).thenReturn(name);
    }

    private void addFourPlayers() {
        game.addPlayer(mockPlayer1);
        game.addPlayer(mockPlayer2);
        game.addPlayer(mockPlayer3);
        game.addPlayer(mockPlayer4);
    }

    @Nested
    @DisplayName("Player & Roster Management")
    class PlayerTests {

        @Test
        @DisplayName("addPlayer rejects nulls and duplicates")
        void addPlayer_Validation() {
            game.addPlayer(mockPlayer1);
            assertThrows(IllegalArgumentException.class, () -> game.addPlayer(null));
            assertThrows(IllegalArgumentException.class, () -> game.addPlayer(mockPlayer1));
        }

        @Test
        @DisplayName("getPlayers throws if active table requirements (<4) aren't met")
        void getPlayers_ThrowsIfNotEnoughPlayers() {
            game.addPlayer(mockPlayer1);
            assertThrows(IllegalStateException.class, () -> game.getPlayers(), "Should crash if asking for active table with < 4 players");
        }

        @Test
        @DisplayName("Data accessors (getAllPlayers, getPlayerIds, Maps) return accurate data")
        void playerAccessors_WorkCorrectly() {
            addFourPlayers();
            game.addPlayer(mockPlayer5); // Roster size 5

            assertEquals(5, game.getTotalPlayerCount());
            assertEquals(5, game.getAllPlayers().size());
            assertEquals(4, game.getPlayers().size(), "Active table must strictly return 4");

            List<PlayerId> ids = game.getPlayerIds();
            assertTrue(ids.containsAll(List.of(id1, id2, id3, id4, id5)));

            Map<PlayerId, String> namesMap = game.getPlayerNamesMap();
            assertEquals("Alice", namesMap.get(id1));
        }

        @Test
        @DisplayName("removePlayer enforces minimum roster size and safely un-assigns dealer")
        void removePlayer_Logic() {
            addFourPlayers();
            game.addPlayer(mockPlayer5);

            game.setDealerPlayer(mockPlayer1);
            game.removePlayer(mockPlayer1); // Allowed because 5 > 4

            assertEquals(4, game.getTotalPlayerCount());
            assertEquals(mockPlayer2, game.getDealerPlayer(), "Dealer should auto-shift to the next active player");

            // Verify constraint: Can't drop below 4
            assertThrows(IllegalStateException.class, () -> game.removePlayer(mockPlayer2));
        }

        @Test
        @DisplayName("resetPlayers clears the roster entirely")
        void resetPlayers_ClearsEverything() {
            addFourPlayers();
            game.setDealerPlayer(mockPlayer1);

            game.resetPlayers();

            assertEquals(0, game.getTotalPlayerCount());
            assertNull(game.getDealerPlayer());
        }
    }

    @Nested
    @DisplayName("Dealer Management")
    class DealerTests {

        @Test
        @DisplayName("Dealer assignment protects against invalid players")
        void setDealer_Validation() {
            addFourPlayers();
            assertThrows(IllegalArgumentException.class, () -> game.setDealerPlayer(mockPlayer5), "Cannot deal to a player not at the active table");
        }

        @Test
        @DisplayName("setRandomDealer successfully picks an active player")
        void setRandomDealer_Success() {
            assertThrows(IllegalStateException.class, () -> game.setRandomDealer()); // Empty check

            addFourPlayers();
            game.setRandomDealer();
            assertNotNull(game.getDealerPlayer());
            assertTrue(game.getPlayers().contains(game.getDealerPlayer()));
        }

        @Test
        @DisplayName("advanceDealer rotates dealer token to the next player")
        void advanceDealer_CyclesToken() {
            addFourPlayers();
            game.setDealerPlayer(mockPlayer4);

            game.advanceDealer();
            assertEquals(mockPlayer1, game.getDealerPlayer(), "Dealer must rotate clockwise, looping back to the start.");
        }
    }

    @Nested
    @DisplayName("Card Dealing & Deck Interaction")
    class DeckTests {
        @Test
        @DisplayName("dealCards protects against bad states")
        void dealCards_Validation() {
            assertThrows(IllegalStateException.class, () -> game.dealCards(), "Cannot deal without a deck");
            game.setDeck(mock(Deck.class));
            assertThrows(IllegalStateException.class, () -> game.dealCards(), "Cannot deal without 4 players");
        }

        @Test
        @DisplayName("dealCards shuffles, distributes cards, and returns correct trump suit")
        void dealCards_DistributesToAll() {
            addFourPlayers();
            Deck mockDeck = mock(Deck.class);
            game.setDeck(mockDeck);

            List<Card> hand = List.of(new Card(Suit.SPADES, Rank.ACE));
            when(mockDeck.deal(Deck.DealType.WHIST)).thenReturn(List.of(hand, hand, hand, hand));

            Suit resultingTrump = game.dealCards();

            verify(mockDeck).shuffle();
            verify(mockPlayer1).setHand(hand);
            verify(mockPlayer4).setHand(hand);
            assertEquals(Suit.SPADES, resultingTrump);
        }

        @Test
        @DisplayName("Deck accessors get/set correctly")
        void deckAccessors() {
            assertNull(game.getDeck());
            Deck mockDeck = mock(Deck.class);
            game.setDeck(mockDeck);
            assertEquals(mockDeck, game.getDeck());
        }
    }

    @Nested
    @DisplayName("Round Initialization, Tracking & Win Logic")
    class RoundTests {

        @Test
        @DisplayName("Round management (add, remove, get, reset, current) works properly")
        void roundManagement() {
            Round mockRound1 = mock(Round.class);
            Round mockRound2 = mock(Round.class);

            game.addRound(mockRound1);
            game.addRound(mockRound2);

            assertEquals(2, game.getRounds().size());
            assertEquals(mockRound2, game.getCurrentRound());

            game.removeRound(mockRound1);
            assertEquals(1, game.getRounds().size());
            assertEquals(mockRound2, game.getCurrentRound());

            game.resetRounds();
            assertTrue(game.getRounds().isEmpty());
            assertNull(game.getCurrentRound());
        }

        @Test
        @DisplayName("initializeNextRound throws on missing players")
        void initRound_Validation() {
            assertThrows(IllegalStateException.class, () -> game.initializeNextRound(mockPlayer1));
            addFourPlayers();
            assertThrows(IllegalArgumentException.class, () -> game.initializeNextRound(mockPlayer5), "Starter must be active");
        }

        @Test
        @DisplayName("initializeNextRound applies x2 multiplier if previous round passed")
        void initRound_MultiplierLogic() {
            addFourPlayers();

            Round pastRound = mock(Round.class);
            Bid passBid = mock(PassBid.class);
            when(passBid.getType()).thenReturn(BidType.PASS);
            when(pastRound.getHighestBid()).thenReturn(passBid);
            game.addRound(pastRound);

            game.initializeNextRound(mockPlayer1);

            assertEquals(2, game.getRounds().size());
            assertEquals(2, game.getCurrentRound().getMultiplier(), "Multiplier must double after a PASS round");
        }

        @Test
        @DisplayName("getLastRoundWinner degrades gracefully or returns actual winner")
        void getLastRoundWinner_Logic() {
            assertNull(game.getLastRoundWinner(), "Null when no rounds");

            Round mockRoundDraw = mock(Round.class);
            when(mockRoundDraw.getWinningPlayers()).thenReturn(List.of());
            game.addRound(mockRoundDraw);

            assertNull(game.getLastRoundWinner(), "Null when round ended in a draw");

            Round mockRoundWin = mock(Round.class);
            when(mockRoundWin.getWinningPlayers()).thenReturn(List.of(mockPlayer1));
            game.addRound(mockRoundWin);

            assertEquals(mockPlayer1, game.getLastRoundWinner(), "Returns winner of the last round");
        }
    }

    @Nested
    @DisplayName("Game Queue & Score Calibration")
    class AdvancedFlowTests {

        @Test
        @DisplayName("rotateActivePlayers cycles the dealer out of the active 4 seats")
        void rotateActivePlayers_Logic() {
            addFourPlayers();
            game.addPlayer(mockPlayer5);
            game.setDealerPlayer(mockPlayer1); // Alice is dealer

            game.rotateActivePlayers();

            // Alice was rotated out. Bob (P2) inherits the physical dealer seat in the active table.
            assertEquals(mockPlayer2, game.getDealerPlayer());
            List<Player> activeTable = game.getPlayers();
            assertFalse(activeTable.contains(mockPlayer1), "Alice should no longer be in the active 4 seats");
            assertTrue(activeTable.contains(mockPlayer5), "Eve should have rotated into the active 4 seats");
        }

        @Test
        @DisplayName("rotateActivePlayers does nothing if table is 4 players")
        void rotateActivePlayers_NoOp() {
            addFourPlayers();
            game.setDealerPlayer(mockPlayer1);
            game.rotateActivePlayers();
            assertEquals(mockPlayer1, game.getDealerPlayer()); // Unchanged
        }

        @Test
        @DisplayName("recalibrateScores rebuilding logic works perfectly")
        void recalibrateScores_Success() {
            addFourPlayers();
            lenient().when(mockPlayer1.getScore()).thenReturn(50);

            Round mockRound = mock(Round.class);
            when(mockRound.getPlayers()).thenReturn(List.of(mockPlayer1, mockPlayer2, mockPlayer3, mockPlayer4));
            when(mockRound.getScoreDeltas()).thenReturn(List.of(10, 0, 0, 0));
            game.addRound(mockRound);

            game.recalibrateScores();

            verify(mockPlayer1).updateScore(-50);
            verify(mockPlayer1).updateScore(10);
        }
    }

    @Nested
    @DisplayName("State Machine & Observers")
    class StateAndObserverTests {

        @Test
        @DisplayName("State machine transitions execute properly")
        void stateTransitions() {
            addFourPlayers();
            game.setDealerPlayer(mockPlayer1);

            Deck deck = mock(Deck.class);
            game.setDeck(deck);

            List<Card> dummyHand = List.of(new Card(Suit.HEARTS, Rank.ACE));
            when(deck.deal(Deck.DealType.WHIST)).thenReturn(List.of(dummyHand, dummyHand, dummyHand, dummyHand));

            assertTrue(game.isOver());

            game.startGame();

            assertFalse(game.isOver());
            assertInstanceOf(BidState.class, getInternalState(game));

            game.startCount();
            assertInstanceOf(CountState.class, getInternalState(game));
        }

        @Test
        @DisplayName("executeState (with and without args) and nextState delegate to the active State")
        void stateDelegation() {
            State mockState = mock(State.class);
            State mockNextState = mock(State.class);
            StateStep mockStep = mock(StateStep.class);
            GameCommand mockCommand = mock(GameCommand.BidCommand.class);

            when(mockState.nextState()).thenReturn(mockNextState);
            when(mockState.executeState()).thenReturn(mockStep);

            setInternalState(game, mockState);

            // Test executeState() (no-args)
            assertEquals(mockStep, game.executeState());
            verify(mockState).executeState();

            // Test executeState(command)
            game.executeState(mockCommand);
            verify(mockState).executeState(mockCommand);

            // Test nextState()
            game.nextState();
            assertEquals(mockNextState, getInternalState(game));
        }

        @Test
        @DisplayName("Observer pattern correctly distributes UI events")
        void observers_BroadcastProperly() {
            addFourPlayers();
            GameObserver mockObs = mock(GameObserver.class);
            game.addObserver(mockObs);

            game.notifyRoundStarted();
            verify(mockObs).onRoundStarted(anyList());

            game.notifyTrumpDetermined(Suit.HEARTS);
            verify(mockObs).onTrumpDetermined(Suit.HEARTS);

            BidTurn mockBid = mock(BidTurn.class);
            game.notifyBidPlaced(mockBid);
            verify(mockObs).onBidPlaced(mockBid);
        }
    }

    // --- Reflection Helper to read/write internal states ---
    private State getInternalState(WhistGame game) {
        try {
            java.lang.reflect.Field field = WhistGame.class.getDeclaredField("state");
            field.setAccessible(true);
            return (State) field.get(game);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed", e);
        }
    }

    private void setInternalState(WhistGame game, State state) {
        try {
            java.lang.reflect.Field field = WhistGame.class.getDeclaredField("state");
            field.setAccessible(true);
            field.set(game, state);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed", e);
        }
    }
}