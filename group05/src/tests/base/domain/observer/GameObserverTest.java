package base.domain.observer;

import base.domain.card.Suit;
import base.domain.player.PlayerId;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

@DisplayName("GameObserver Interface Default Methods")
class GameObserverTest {

    // Instantiate an anonymous class to inherit and test the default interface logic
    private final GameObserver observer = new GameObserver() {};

    @Test
    @DisplayName("onBidPlaced default implementation is a safe no-op")
    void onBidPlaced_DefaultIsNoOp() {
        BidTurn mockTurn = mock(BidTurn.class);

        // Assert it does not crash on valid input
        assertDoesNotThrow(() -> observer.onBidPlaced(mockTurn));

        // Assert it does not crash on null input (defensive edge case)
        assertDoesNotThrow(() -> observer.onBidPlaced(null));
    }

    @Test
    @DisplayName("onTrumpDetermined default implementation is a safe no-op")
    void onTrumpDetermined_DefaultIsNoOp() {
        assertDoesNotThrow(() -> observer.onTrumpDetermined(Suit.HEARTS));
        assertDoesNotThrow(() -> observer.onTrumpDetermined(null));
    }

    @Test
    @DisplayName("onTurnPlayed default implementation is a safe no-op")
    void onTurnPlayed_DefaultIsNoOp() {
        PlayTurn mockTurn = mock(PlayTurn.class);

        assertDoesNotThrow(() -> observer.onTurnPlayed(mockTurn));
        assertDoesNotThrow(() -> observer.onTurnPlayed(null));
    }

    @Test
    @DisplayName("onRoundStarted default implementation is a safe no-op")
    void onRoundStarted_DefaultIsNoOp() {
        List<PlayerId> players = List.of(mock(PlayerId.class), mock(PlayerId.class));

        assertDoesNotThrow(() -> observer.onRoundStarted(players));
        assertDoesNotThrow(() -> observer.onRoundStarted(null));
    }
}