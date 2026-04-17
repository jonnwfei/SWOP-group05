package base.domain.results;

import base.domain.bid.BidType;
import base.domain.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Bid Selection Result Tests")
class BidSelectionResultTest {

    @Mock
    private Player mockPlayer1;

    @Mock
    private Player mockPlayer2;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    private BidType[] validBids() {
        return new BidType[]{BidType.PASS, BidType.SOLO};
    }

    @Nested
    @DisplayName("Constructor & Validation")
    class ConstructorTests {

        @Test
        @DisplayName("Should successfully create result with valid inputs and immutable list")
        void shouldAcceptValidInputs() {
            List<Player> players = List.of(mockPlayer1, mockPlayer2);
            BidType[] bids = validBids();

            BidSelectionResult result = new BidSelectionResult(bids, players);

            assertThat(result.availableBids()).containsExactly(BidType.PASS, BidType.SOLO);
            assertThat(result.players()).hasSize(2).contains(mockPlayer1, mockPlayer2);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if availableBids is null or empty")
        void shouldRejectInvalidBids() {
            List<Player> players = List.of(mockPlayer1);

            assertThatThrownBy(() -> new BidSelectionResult(null, players))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new BidSelectionResult(new BidType[]{}, players))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if players list is null or empty")
        void shouldRejectInvalidPlayers() {
            BidType[] bids = validBids();

            assertThatThrownBy(() -> new BidSelectionResult(bids, null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new BidSelectionResult(bids, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Defensive Copying & Encapsulation")
    class MutabilityTests {

        @Test
        @DisplayName("Internal array should be protected from external modification of the input array")
        void shouldProtectAgainstInputMutation() {
            BidType[] inputBids = {BidType.PASS};
            BidSelectionResult result = new BidSelectionResult(inputBids, List.of(mockPlayer1));

            // Mutate the array we passed in
            inputBids[0] = BidType.ABONDANCE_10;

            // Internal state should remain PASS
            assertThat(result.availableBids()).containsExactly(BidType.PASS);
        }

        @Test
        @DisplayName("Internal array should be protected from modification via the accessor")
        void shouldProtectAgainstAccessorMutation() {
            BidSelectionResult result = new BidSelectionResult(validBids(), List.of(mockPlayer1));

            // Mutate the array we got back from the getter
            BidType[] externalBids = result.availableBids();
            externalBids[0] = BidType.ABONDANCE_9;

            // Internal state should remain PASS
            assertThat(result.availableBids()[0]).isEqualTo(BidType.PASS);
        }

        @Test
        @DisplayName("Players list should be an unmodifiable copy")
        void shouldReturnUnmodifiablePlayersList() {
            BidSelectionResult result = new BidSelectionResult(validBids(), List.of(mockPlayer1));

            assertThatThrownBy(() -> result.players().add(mockPlayer2))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Record Property Overrides")
    class PropertyTests {

        @Test
        @DisplayName("toString should clearly show field contents")
        void shouldHaveDescriptiveToString() {
            BidSelectionResult result = new BidSelectionResult(new BidType[]{BidType.PASS}, List.of(mockPlayer1));

            assertThat(result.toString())
                    .contains("BidSelectionResult")
                    .contains("availableBids")
                    .contains("players");
        }
    }
}