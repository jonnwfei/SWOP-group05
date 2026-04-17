package base.domain;

import base.domain.card.*;
import base.domain.commands.BidCommand;
import base.domain.commands.GameCommand;
import base.domain.deck.Deck;
import base.domain.observer.GameObserver;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.round.Round;
import base.domain.states.BidState;
import base.domain.states.State;
import base.domain.turn.PlayTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Whist Game Aggregate Root")
class WhistGameTest {

    private WhistGame game;
    private Player mockPlayer1;
    private Player mockPlayer2;
    private PlayerId id1;
    private PlayerId id2;

    @BeforeEach
    void setUp() {
        game = new WhistGame();

        mockPlayer1 = mock(Player.class);
        mockPlayer2 = mock(Player.class);
        id1 = new PlayerId();
        id2 = new PlayerId();

        lenient().when(mockPlayer1.getId()).thenReturn(id1);
        lenient().when(mockPlayer2.getId()).thenReturn(id2);
    }

    @Nested
    @DisplayName("Player & Roster Management")
    class PlayerTests {
        @Test
        @DisplayName("getPlayerById returns the correct player or throws")
        void getPlayerById_Logic() {
            game.addPlayer(mockPlayer1);

            assertEquals(mockPlayer1, game.getPlayerById(id1));
            // Verifying negative scenario: illegal input handled defensively 
            assertThrows(IllegalStateException.class, () -> game.getPlayerById(new PlayerId()));
        }

        @Test
        @DisplayName("getNextPlayer correctly calculates the next person in seating order")
        void getNextPlayer_ModuloRotation() {
            // Clockwise fashion is the standard for Whist bidding and play 
            game.addPlayer(mockPlayer1);
            game.addPlayer(mockPlayer2);

            assertEquals(mockPlayer2, game.getNextPlayer(mockPlayer1));
            assertEquals(mockPlayer1, game.getNextPlayer(mockPlayer2)); // Loop back (Modulo logic)
        }
    }

    @Nested
    @DisplayName("Dealer Management")
    class DealerTests {
        @Test
        @DisplayName("advanceDealer moves the dealer token to the next player")
        void advanceDealer_CyclesToken() {
            game.addPlayer(mockPlayer1);
            game.addPlayer(mockPlayer2);
            game.setDealerPlayer(mockPlayer1);

            game.advanceDealer();

            assertEquals(mockPlayer2, game.getDealerPlayer(), "Dealer must rotate clockwise[cite: 156].");
        }
    }

    @Nested
    @DisplayName("Card Dealing & Deck Interaction")
    class DeckTests {
        @Test
        @DisplayName("dealCards shuffles and distributes cards to all 4 players")
        void dealCards_DistributesToAll() {
            // Arrange
            Deck mockDeck = mock(Deck.class);
            Player p3 = mock(Player.class);
            Player p4 = mock(Player.class);
            game.addPlayer(mockPlayer1); game.addPlayer(mockPlayer2);
            game.addPlayer(p3); game.addPlayer(p4);
            game.setDeck(mockDeck);

            List<Card> hand = List.of(new Card(Suit.SPADES, Rank.ACE));
            // Whist requires distributing a standard deck of 52 cards [cite: 156]
            when(mockDeck.deal()).thenReturn(List.of(hand, hand, hand, hand));

            // Act
            game.dealCards();

            // Assert
            verify(mockDeck).shuffle();
            verify(mockPlayer1).setHand(any());
            verify(p4).setHand(any());
        }
    }

    @Nested
    @DisplayName("State Machine Delegation")
    class StateTests {
        @Test
        @DisplayName("executeState(command) delegates strictly to the current state")
        void executeState_Delegation() {
            // Test verifies delegation to maintain Low Representational Gap [cite: 327]
            State mockState = mock(BidState.class);
            setInternalState(game, mockState);
            GameCommand mockCommand = mock(BidCommand.class);

            game.executeState(mockCommand);

            verify(mockState).executeState(mockCommand);
        }
    }

    @Nested
    @DisplayName("Round History & Winners")
    class RoundTests {
        @Test
        @DisplayName("getLastRoundWinner retrieves winning player from the most recent round")
        void getLastRoundWinner_Delegation() {
            Round mockRound = mock(Round.class);
            when(mockRound.getWinningPlayers()).thenReturn(List.of(mockPlayer1));
            game.addRound(mockRound);

            assertEquals(mockPlayer1, game.getLastRoundWinner());
        }
    }

    @Nested
    @DisplayName("Observer Pattern (Events)")
    class ObserverTests {
        @Test
        @DisplayName("notifyTurnPlayed broadcasts event to all registered observers")
        void notifyTurnPlayed_Broadcasting() {
            GameObserver mockObs = mock(GameObserver.class);
            game.addObserver(mockObs);
            PlayTurn turn = new PlayTurn(id1, new Card(Suit.HEARTS, Rank.KING));

            game.notifyTurnPlayed(turn);

            verify(mockObs).onTurnPlayed(turn);
        }
    }

    // --- Reflection Helper to inject mock states ---
    private void setInternalState(WhistGame game, State state) {
        try {
            java.lang.reflect.Field field = WhistGame.class.getDeclaredField("state");
            field.setAccessible(true);
            field.set(game, state);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed during WhistGameState injection.", e);
        }
    }
}