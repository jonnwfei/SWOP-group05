package base.domain.strategy;

import base.domain.bid.Bid;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@DisplayName("SmartBot AI Strategy Rules & Tactics")
class SmartBotStrategyTest {
    private SmartBotStrategy strategy;
    private final PlayerId botId = new PlayerId();

    // 1. Remove the @Mock annotations
    private SmartBotMemory mockMemory;
    private Random mockRandom;

    @BeforeEach
    void setUp() throws Exception {
        // 2. Initialize the mocks manually before passing them to the strategy
        mockMemory = mock(SmartBotMemory.class);
        mockRandom = mock(Random.class);

        strategy = new SmartBotStrategy(botId);

        // INJECTION REFLECTION: Swap the internal Memory and Random objects with our Mocks
        Field memoryField = SmartBotStrategy.class.getDeclaredField("memory");
        memoryField.setAccessible(true);
        memoryField.set(strategy, mockMemory);

        Field randomField = SmartBotStrategy.class.getDeclaredField("random");
        randomField.setAccessible(true);
        randomField.set(strategy, mockRandom);

        // Default the Random mock to always select the first item in any filtered list
        lenient().when(mockRandom.nextInt(anyInt())).thenReturn(0);
    }

    @Nested
    @DisplayName("Bidding Phase Heuristics")
    class BiddingTests {

        @Test
        @DisplayName("Should bid OPEN MISERIE if the highest card is a 7 or lower")
        void bidsOpenMiserie() {
            List<Card> hand = List.of(
                    new Card(Suit.HEARTS, Rank.TWO),
                    new Card(Suit.SPADES, Rank.SEVEN)
            );

            Bid bid = strategy.determineBid(botId, hand);

            assertEquals(BidType.OPEN_MISERIE, bid.getType());
        }

        @Test
        @DisplayName("Should bid MISERIE if the highest card is between 8 and 10")
        void bidsMiserie() {
            List<Card> hand = List.of(
                    new Card(Suit.HEARTS, Rank.TWO),
                    new Card(Suit.SPADES, Rank.TEN)
            );

            Bid bid = strategy.determineBid(botId, hand);

            assertEquals(BidType.MISERIE, bid.getType());
        }

        @Test
        @DisplayName("Should bid ABONDANCE 9 if evaluating 9 guaranteed tricks")
        void bidsAbondance9() {
            // Hand with 9 Hearts (including an Ace to prevent Miserie evaluation)
            List<Card> hand = List.of(
                    new Card(Suit.HEARTS, Rank.ACE), new Card(Suit.HEARTS, Rank.KING),
                    new Card(Suit.HEARTS, Rank.QUEEN), new Card(Suit.HEARTS, Rank.JACK),
                    new Card(Suit.HEARTS, Rank.TEN), new Card(Suit.HEARTS, Rank.NINE),
                    new Card(Suit.HEARTS, Rank.EIGHT), new Card(Suit.HEARTS, Rank.SEVEN),
                    new Card(Suit.HEARTS, Rank.SIX),
                    new Card(Suit.CLUBS, Rank.TWO), new Card(Suit.CLUBS, Rank.THREE)
            );

            Bid bid = strategy.determineBid(botId, hand);

            assertEquals(BidType.ABONDANCE_9, bid.getType());
            assertEquals(Suit.HEARTS, bid.determineTrump(Suit.HEARTS), "Should correctly identify Hearts as the best trump suit.");
        }

        @Test
        @DisplayName("Should bid ACCEPTANCE if 3+ tricks expected and an active proposal exists")
        void bidsAcceptance() {
            List<Card> hand = List.of(new Card(Suit.HEARTS, Rank.ACE), new Card(Suit.SPADES, Rank.KING), new Card(Suit.CLUBS, Rank.QUEEN));

            when(mockMemory.getCurrentTrump()).thenReturn(Suit.DIAMONDS);
            when(mockMemory.hasActiveProposal()).thenReturn(true);

            Bid bid = strategy.determineBid(botId, hand);

            assertEquals(BidType.ACCEPTANCE, bid.getType());
        }

        @Test
        @DisplayName("Should bid PASS if the hand is weak (0-2 tricks)")
        void bidsPassWhenWeak() {
            List<Card> hand = List.of(new Card(Suit.HEARTS, Rank.ACE), new Card(Suit.SPADES, Rank.TWO)); // Only 1 trick expected

            when(mockMemory.getCurrentTrump()).thenReturn(Suit.DIAMONDS);

            Bid bid = strategy.determineBid(botId, hand);

            assertEquals(BidType.PASS, bid.getType());
        }
    }

    @Nested
    @DisplayName("Play Phase Tactics: NORMAL Mode")
    class NormalTacticTests {

        @BeforeEach
        void setupNormalMode() {
            // Ensure no miserie bid is active
            lenient().when(mockMemory.getHighestBid()).thenReturn(null);
        }

        @Test
        @DisplayName("Should play guaranteed winner if leading the trick")
        void playsWinnerWhenLeading() {
            Card winningCard = new Card(Suit.HEARTS, Rank.ACE);
            List<Card> hand = List.of(winningCard, new Card(Suit.HEARTS, Rank.TWO));

            when(mockMemory.isLeadPlayer()).thenReturn(true);
            when(mockMemory.isHighestUnplayedCardInSuit(winningCard)).thenReturn(true);

            Card played = strategy.chooseCardToPlay(hand, null);

            assertEquals(winningCard, played);
        }

        @Test
        @DisplayName("Should conserve high cards and play low if the team is already winning")
        void playsLowIfPartnerWinning() {
            Card lowHeart = new Card(Suit.HEARTS, Rank.TWO);
            Card highHeart = new Card(Suit.HEARTS, Rank.ACE);
            List<Card> hand = List.of(highHeart, lowHeart);

            when(mockMemory.isLeadPlayer()).thenReturn(false);
            when(mockMemory.isTeamWinning(botId)).thenReturn(true);

            Card played = strategy.chooseCardToPlay(hand, Suit.HEARTS);

            assertEquals(lowHeart, played, "Should conserve the Ace because the partner is already winning.");
        }

        @Test
        @DisplayName("Should play lowest trump if void in lead suit and team is losing")
        void ruffsWithLowestTrumpWhenVoid() {
            Card diamondTwo = new Card(Suit.DIAMONDS, Rank.TWO); // Non-trump discard
            Card heartThree = new Card(Suit.HEARTS, Rank.THREE); // Lowest Trump
            Card heartAce = new Card(Suit.HEARTS, Rank.ACE);     // Highest Trump
            List<Card> hand = List.of(diamondTwo, heartThree, heartAce);

            when(mockMemory.isLeadPlayer()).thenReturn(false);
            when(mockMemory.isTeamWinning(botId)).thenReturn(false);
            when(mockMemory.getCurrentTrump()).thenReturn(Suit.HEARTS);

            // Lead is SPADES, bot is void
            Card played = strategy.chooseCardToPlay(hand, Suit.SPADES);

            assertEquals(heartThree, played, "Should ruff the trick with the lowest possible trump card.");
        }
    }

    @Nested
    @DisplayName("Play Phase Tactics: MISERIE Mode")
    class MiserieTacticTests {

        @BeforeEach
        void setupMiserieMode() {
            // Force the strategy to recognize itself as the Miserie player
            BidTurn mockBid = mock(BidTurn.class);
            lenient().when(mockBid.bidType()).thenReturn(BidType.MISERIE);
            lenient().when(mockBid.playerId()).thenReturn(botId);

            lenient().when(mockMemory.getHighestBid()).thenReturn(mockBid);
        }

        @Test
        @DisplayName("Should play the highest possible safe card that dodges winning")
        void playsHighestSafeCard() {
            Card heartTwo = new Card(Suit.HEARTS, Rank.TWO);
            Card heartNine = new Card(Suit.HEARTS, Rank.NINE);
            Card heartAce = new Card(Suit.HEARTS, Rank.ACE);
            List<Card> hand = List.of(heartTwo, heartNine, heartAce);

            when(mockMemory.isLeadPlayer()).thenReturn(false);

            // Assume the opponent played a Jack.
            PlayTurn playTurn = new PlayTurn(new PlayerId(), new Card(Suit.HEARTS, Rank.JACK));
            when(mockMemory.getCurrentWinningTurn()).thenReturn(playTurn);

            Card played = strategy.chooseCardToPlay(hand, Suit.HEARTS);

            // The Nine is the highest card that safely stays under the Jack
            assertEquals(heartNine, played, "Must dump the highest card that won't take the trick.");
        }
    }

    @Nested
    @DisplayName("Play Phase Tactics: ANTI-MISERIE Mode")
    class AntiMiserieTacticTests {

        private final PlayerId opponentId = new PlayerId();

        @BeforeEach
        void setupAntiMiserieMode() {
            // Force the strategy to recognize the OPPONENT as the Miserie player
            BidTurn mockBid = mock(BidTurn.class);
            lenient().when(mockBid.bidType()).thenReturn(BidType.MISERIE);
            lenient().when(mockBid.playerId()).thenReturn(opponentId);

            lenient().when(mockMemory.getHighestBid()).thenReturn(mockBid);
        }

        @Test
        @DisplayName("Should play the lowest safe card to force the Miserie player to win")
        void playsLowestSafeCardToForceWin() {
            Card heartTwo = new Card(Suit.HEARTS, Rank.TWO);
            Card heartEight = new Card(Suit.HEARTS, Rank.EIGHT);
            Card heartKing = new Card(Suit.HEARTS, Rank.KING);
            List<Card> hand = List.of(heartTwo, heartEight, heartKing);

            // The Miserie opponent has played a Ten and is currently winning
            Card opponentCard = new Card(Suit.HEARTS, Rank.TEN);
            when(mockMemory.hasPlayerActedInCurrentTrick(opponentId)).thenReturn(true);
            when(mockMemory.calculateCurrentWinnerId()).thenReturn(opponentId);
            when(mockMemory.getCardPlayedBy(opponentId)).thenReturn(opponentCard);

            Card played = strategy.chooseCardToPlay(hand, Suit.HEARTS);

            // Two is the lowest card the bot has that is still safely UNDER the Ten.
            // If the bot played the King, the bot would win the trick, letting the opponent off the hook.
            assertEquals(heartTwo, played, "Must play a card under the opponent's card to force them to take the trick.");
        }
    }
}