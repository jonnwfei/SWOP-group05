package base.domain.strategy;

import base.domain.observer.GameEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("Strategy Interface Default Methods")
class StrategyTest {

    @Test
    @DisplayName("onJoinGame default implementation should be a safe no-op")
    void onJoinGame_DefaultIsNoOp() {
        Strategy strategy = new HumanStrategy();

        GameEventPublisher mockPublisher = mock(GameEventPublisher.class);

        assertDoesNotThrow(() -> strategy.onJoinGame(mockPublisher));

        verifyNoInteractions(mockPublisher);
    }
}