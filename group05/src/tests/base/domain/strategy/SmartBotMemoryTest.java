package base.domain.strategy;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.PlayerId;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Smart Bot Memory (Observer & Logic)")
class SmartBotMemoryTest {

    private SmartBotMemory memory;
    private final PlayerId p1 = new PlayerId("player-1");
    private final PlayerId p2 = new PlayerId("player-2");
    private final PlayerId p3 = new PlayerId("player-3");
    private final PlayerId p4 = new PlayerId("player-4");

    @BeforeEach
    void setUp() {
        memory = new SmartBotMemory();
    }

    @Nested
    @DisplayName("Observer Event Handling")
    class EventHandlingTests {

        @Test
        @DisplayName("onRoundStarted should reset all memory buffers")
        void shouldResetMemoryOnNewRound() {
            memory.onTrumpDetermined(Suit.HEARTS);
            memory.onBidPlaced(new BidTurn(p1, BidType.SOLO));

            memory.onRoundStarted(List.of(p1, p2, p3, p4));

            assertThat(memory.getCurrentTrump()).isNull();
            assertThat(memory.getHighestBid()).isNull();
            assertThat(memory.isLeadPlayer()).isTrue();
        }

        @Test
        @DisplayName("onTurnPlayed should track unplayed cards (Card Counting)")
        void shouldTrackUnplayedCards() {
            Card aceOfSpades = new Card(Suit.SPADES, Rank.ACE);
            memory.onTurnPlayed(new PlayTurn(p1, aceOfSpades));

            // The card is no longer the highest unplayed because it IS played
            // (or more accurately, it's removed from the unplayed pool)
            assertThat(memory.isHighestUnplayedCardInSuit(aceOfSpades)).isTrue();
            // ^ Note: isHighestUnplayedCardInSuit logic checks the pool.
            // If the Ace is gone, the King becomes the new highest.

            Card kingOfSpades = new Card(Suit.SPADES, Rank.KING);
            assertThat(memory.isHighestUnplayedCardInSuit(kingOfSpades)).isTrue();
        }
    }

    @Nested
    @DisplayName("Trick Evaluation Logic")
    class TrickLogicTests {

        @BeforeEach
        void initTrick() {
            memory.onTrumpDetermined(Suit.CLUBS); // Clubs are trump
        }

        @Test
        @DisplayName("getCurrentWinningTurn identifies high card of lead suit")
        void shouldIdentifyWinningLeadSuit() {
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.TEN))); // Lead
            memory.onTurnPlayed(new PlayTurn(p2, new Card(Suit.HEARTS, Rank.QUEEN))); // Higher

            PlayTurn winner = memory.getCurrentWinningTurn();
            assertThat(winner.playerId()).isEqualTo(p2);
        }

        @Test
        @DisplayName("getCurrentWinningTurn identifies trump winner")
        void shouldIdentifyTrumpWinner() {
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.ACE))); // High lead
            memory.onTurnPlayed(new PlayTurn(p2, new Card(Suit.CLUBS, Rank.TWO)));  // Low Trump

            assertThat(memory.calculateCurrentWinnerId()).isEqualTo(p2);
        }
    }

    @Nested
    @DisplayName("Partnership & Team Logic")
    class TeamLogicTests {

        @Test
        @DisplayName("isTeamWinning correctly identifies partner in Proposal/Acceptance")
        void shouldIdentifyPartnershipWinning() {
            // Arrange: P1 Proposes, P3 Accepts. They are now a team.
            memory.onBidPlaced(new BidTurn(p1, BidType.PROPOSAL));
            memory.onBidPlaced(new BidTurn(p2, BidType.PASS));
            memory.onBidPlaced(new BidTurn(p3, BidType.ACCEPTANCE));
            memory.onTrumpDetermined(Suit.HEARTS);

            // Act: P1 (Partner) plays a winning card
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.ACE)));

            // Assert: P3 should recognize that their team is winning
            assertThat(memory.isTeamWinning(p3)).isTrue();
            // Assert: P2 (Opponent) should recognize their team is NOT winning
            assertThat(memory.isTeamWinning(p2)).isFalse();
        }

        @Test
        @DisplayName("isTeamWinning throws exception if no bids recorded")
        void shouldThrowIfNoBids() {
            memory.onTurnPlayed(new PlayTurn(p1, new Card(Suit.HEARTS, Rank.ACE)));

            assertThatThrownBy(() -> memory.isTeamWinning(p1))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Card Counting Logic")
    class CardCountingTests {

        @Test
        @DisplayName("isHighestUnplayedCardInSuit detects if better cards are gone")
        void shouldDetectMasterCard() {
            Card kingOfHearts = new Card(Suit.HEARTS, Rank.KING);
            Card aceOfHearts = new Card(Suit.HEARTS, Rank.ACE);

            // Initially, King is not the master because Ace is out there
            assertThat(memory.isHighestUnplayedCardInSuit(kingOfHearts)).isFalse();

            // Ace is played
            memory.onTurnPlayed(new PlayTurn(p4, aceOfHearts));

            // Now King is the master
            assertThat(memory.isHighestUnplayedCardInSuit(kingOfHearts)).isTrue();
        }
    }
}