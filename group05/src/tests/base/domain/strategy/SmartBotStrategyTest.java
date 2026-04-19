package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.CardMath;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.observer.GameEventPublisher;
import base.domain.player.PlayerId;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmartBot AI Strategy Rules & Tactics")
class SmartBotStrategyTest {

    private SmartBotStrategy strategy;
    private final PlayerId botId = new PlayerId();

    @Mock private SmartBotMemory mockMemory;
    @Mock private Random mockRandom;

    private MockedStatic<CardMath> mockedCardMath;

    @BeforeEach
    void setUp() throws Exception {
        strategy = new SmartBotStrategy(botId);

        // INJECTION REFLECTION: Swap the internal Memory and Random objects with our Mocks
        Field memoryField = SmartBotStrategy.class.getDeclaredField("memory");
        memoryField.setAccessible(true);
        memoryField.set(strategy, mockMemory);

        Field randomField = SmartBotStrategy.class.getDeclaredField("random");
        randomField.setAccessible(true);
        randomField.set(strategy, mockRandom);

        // Default the Random mock to always select the first item in any list to ensure deterministic tests
        lenient().when(mockRandom.nextInt(anyInt())).thenReturn(0);
    }

    @AfterEach
    void tearDown() {
        if (mockedCardMath != null && !mockedCardMath.isClosed()) {
            mockedCardMath.close();
        }
    }

    @Nested
    @DisplayName("Lifecycle & Core Mechanics")
    class LifecycleTests {
        @Test
        @DisplayName("Constructor throws on null PlayerId")
        void constructorNullCheck() {
            assertThrows(IllegalArgumentException.class, () -> new SmartBotStrategy(null));
        }

        @Test
        @DisplayName("getGameObserver returns the internal memory")
        void returnsGameObserver() {
            assertEquals(mockMemory, strategy.getGameObserver());
        }

        @Test
        @DisplayName("onJoinGame registers the observer")
        void registersObserver() {
            GameEventPublisher mockPublisher = mock(GameEventPublisher.class);
            strategy.onJoinGame(mockPublisher);
            verify(mockPublisher).addObserver(mockMemory);
        }

        @Test
        @DisplayName("determineBid throws on null player or empty hand")
        void bidValidation() {
            List<Card> validHand = List.of(new Card(Suit.HEARTS, Rank.TWO));
            assertThrows(IllegalArgumentException.class, () -> strategy.determineBid(null, validHand));
            assertThrows(IllegalArgumentException.class, () -> strategy.determineBid(botId, Collections.emptyList()));
        }

        @Test
        @DisplayName("chooseCardToPlay throws on null or empty hand")
        void playValidation() {
            assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(null, Suit.HEARTS));
            assertThrows(IllegalArgumentException.class, () -> strategy.chooseCardToPlay(Collections.emptyList(), Suit.HEARTS));
        }

        @Test
        @DisplayName("chooseCardToPlay throws IllegalStateException if legalCards filters to empty")
        void defensiveLegalCardsCheck() {
            // Using MockedStatic to force the defensive guard to trigger
            mockedCardMath = Mockito.mockStatic(CardMath.class);
            mockedCardMath.when(() -> CardMath.getLegalCards(anyList(), any())).thenReturn(Collections.emptyList());

            List<Card> hand = List.of(new Card(Suit.HEARTS, Rank.TWO));
            assertThrows(IllegalStateException.class, () -> strategy.chooseCardToPlay(hand, Suit.HEARTS));
        }
    }

    @Nested
    @DisplayName("Bidding Phase Heuristics")
    class BiddingTests {

        @Test
        @DisplayName("Solo bid (13 tricks) takes precedence over forced Troel")
        void soloOverridesTroel() {
            List<Card> hand = new ArrayList<>();
            // 4 Aces (normally forces Troel)
            hand.add(new Card(Suit.HEARTS, Rank.ACE));
            hand.add(new Card(Suit.SPADES, Rank.ACE));
            hand.add(new Card(Suit.DIAMONDS, Rank.ACE));
            hand.add(new Card(Suit.CLUBS, Rank.ACE));

            // Fill rest with Spades >= Jack to guarantee 13 calculated tricks
            for (int i = 0; i < 9; i++) {
                hand.add(new Card(Suit.SPADES, Rank.KING));
            }

            // The bot will evaluate Spades as the best suit, yielding 13 tricks.
            when(mockMemory.getCurrentTrump()).thenReturn(Suit.SPADES);

            // It has 4 Aces, but 13 expected tricks means it MUST bid Solo Slim!
            assertEquals(BidType.SOLO_SLIM, strategy.determineBid(botId, hand).getType());
        }

        // ==========================================
        // EXISTING BIDDING TESTS
        // ==========================================

        @Test
        @DisplayName("Bids OPEN_MISERIE if max card is <= 7")
        void bidsOpenMiserie() {
            List<Card> hand = List.of(new Card(Suit.HEARTS, Rank.TWO), new Card(Suit.SPADES, Rank.SEVEN));
            assertEquals(BidType.OPEN_MISERIE, strategy.determineBid(botId, hand).getType());
        }

        @Test
        @DisplayName("Bids MISERIE if max card is <= 10")
        void bidsMiserie() {
            List<Card> hand = List.of(new Card(Suit.HEARTS, Rank.EIGHT), new Card(Suit.SPADES, Rank.TEN));
            assertEquals(BidType.MISERIE, strategy.determineBid(botId, hand).getType());
        }

        @Test
        @DisplayName("Bids PROPOSAL if tricks >= 5 and no active proposal")
        void bidsProposal() {
            List<Card> hand = generateTrickHand(5);
            when(mockMemory.getCurrentTrump()).thenReturn(Suit.SPADES);
            when(mockMemory.hasActiveProposal()).thenReturn(false);

            assertEquals(BidType.PROPOSAL, strategy.determineBid(botId, hand).getType());
        }

        @Test
        @DisplayName("Bids ACCEPTANCE if tricks >= 3 and active proposal exists")
        void bidsAcceptance() {
            List<Card> hand = generateTrickHand(3);
            when(mockMemory.getCurrentTrump()).thenReturn(Suit.SPADES);
            when(mockMemory.hasActiveProposal()).thenReturn(true);

            assertEquals(BidType.ACCEPTANCE, strategy.determineBid(botId, hand).getType());
        }

        @Test
        @DisplayName("Bids PASS if hand is weak (<3 tricks)")
        void bidsPass() {
            List<Card> hand = generateTrickHand(1); // 1 Trick
            when(mockMemory.getCurrentTrump()).thenReturn(Suit.SPADES);

            assertEquals(BidType.PASS, strategy.determineBid(botId, hand).getType());
        }

        @DisplayName("Abondance Tier mapping routing (9 to 13 tricks)")
        void abondanceRouting() {
            when(mockMemory.getCurrentTrump()).thenReturn(Suit.HEARTS);

            assertEquals(BidType.ABONDANCE_10, strategy.determineBid(botId, generateTrickHand(10)).getType());
            assertEquals(BidType.ABONDANCE_11, strategy.determineBid(botId, generateTrickHand(11)).getType());
            assertEquals(BidType.ABONDANCE_12_OT, strategy.determineBid(botId, generateTrickHand(12)).getType());

            List<Card> flushOfSpades = List.of(
                    new Card(Suit.SPADES, Rank.TWO), new Card(Suit.SPADES, Rank.THREE),
                    new Card(Suit.SPADES, Rank.FOUR), new Card(Suit.SPADES, Rank.FIVE),
                    new Card(Suit.SPADES, Rank.SIX), new Card(Suit.SPADES, Rank.SEVEN),
                    new Card(Suit.SPADES, Rank.EIGHT), new Card(Suit.SPADES, Rank.NINE),
                    new Card(Suit.SPADES, Rank.TEN), new Card(Suit.SPADES, Rank.JACK),
                    new Card(Suit.SPADES, Rank.QUEEN), new Card(Suit.SPADES, Rank.KING),
                    new Card(Suit.SPADES, Rank.ACE)
            );

            Bid soloBid = strategy.determineBid(botId, flushOfSpades);
            assertEquals(BidType.SOLO, soloBid.getType(), "Different trump should yield SOLO");

            // Current trump is SPADES.
            // Result: Bot keeps trump as Spades. (Spades == Spades -> SOLO_SLIM)
            when(mockMemory.getCurrentTrump()).thenReturn(Suit.SPADES);
            Bid soloSlimBid = strategy.determineBid(botId, flushOfSpades);
            assertEquals(BidType.SOLO_SLIM, soloSlimBid.getType(), "Same trump should yield SOLO_SLIM");
        }

        @Test
        @DisplayName("Throws exception on mathematically impossible trick counts (>13)")
        void boundsCheckHighBids() {
            List<Card> impossibleHand = generateTrickHand(14);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> strategy.determineBid(botId, impossibleHand));
            assertTrue(ex.getMessage().contains("Invalid tricks value: 14"));
        }

        private List<Card> generateTrickHand(int tricks) {
            List<Card> hand = new ArrayList<>();
            // Generating Spades cards. Spades will evaluate as the best suit.
            // Every Spade >= Jack (or matching the evaluated best trump) counts as 1 trick.
            for (int i = 0; i < tricks; i++) {
                // Ensure we don't accidentally trigger Troel by adding too many Aces
                if (i < 2) {
                    hand.add(new Card(Suit.SPADES, Rank.ACE));
                } else {
                    hand.add(new Card(Suit.SPADES, Rank.KING));
                }
            }
            // Fill remainder with junk so the size matches but they don't add tricks
            while (hand.size() < 13 && hand.size() < tricks) {
                hand.add(new Card(Suit.CLUBS, Rank.TWO));
            }
            return hand;
        }
    }

    @Nested
    @DisplayName("Play Phase Tactics: NORMAL Mode")
    class NormalTacticTests {

        @BeforeEach
        void setupNormal() {
            lenient().when(mockMemory.getHighestBid()).thenReturn(null);
        }

        @Test
        @DisplayName("Leading: Plays guaranteed winner if available")
        void leadWinner() {
            Card ace = new Card(Suit.HEARTS, Rank.ACE);
            Card two = new Card(Suit.HEARTS, Rank.TWO);

            when(mockMemory.isLeadPlayer()).thenReturn(true);

            lenient().when(mockMemory.isHighestUnplayedCardInSuit(any(Card.class))).thenReturn(false);

            when(mockMemory.isHighestUnplayedCardInSuit(ace)).thenReturn(true);

            assertEquals(ace, strategy.chooseCardToPlay(List.of(two, ace), null));
        }

        @Test
        @DisplayName("Leading: Plays lowest card if no guaranteed winner")
        void leadNoWinner() {
            Card low = new Card(Suit.HEARTS, Rank.TWO);
            Card med = new Card(Suit.HEARTS, Rank.NINE);
            when(mockMemory.isLeadPlayer()).thenReturn(true);
            when(mockMemory.isHighestUnplayedCardInSuit(any())).thenReturn(false);

            assertEquals(low, strategy.chooseCardToPlay(List.of(low, med), null));
        }

        @Test
        @DisplayName("Following: Conserves high cards (plays low) if partner is winning")
        void followPartnerWinning() {
            Card low = new Card(Suit.HEARTS, Rank.TWO);
            when(mockMemory.isLeadPlayer()).thenReturn(false);
            when(mockMemory.isTeamWinning(botId)).thenReturn(true);

            assertEquals(low, strategy.chooseCardToPlay(List.of(new Card(Suit.HEARTS, Rank.ACE), low), Suit.HEARTS));
        }

        @Test
        @DisplayName("Following (Losing): Void in lead, has trump -> Ruff with lowest trump")
        void followLosingVoidHasTrump() {
            Card lowTrump = new Card(Suit.HEARTS, Rank.THREE);
            Card discard = new Card(Suit.DIAMONDS, Rank.TWO);

            when(mockMemory.isLeadPlayer()).thenReturn(false);
            when(mockMemory.isTeamWinning(botId)).thenReturn(false);
            when(mockMemory.getCurrentTrump()).thenReturn(Suit.HEARTS);

            // Lead is SPADES. Hand has Diamonds & Hearts (Trump). It is void in Lead.
            assertEquals(lowTrump, strategy.chooseCardToPlay(List.of(discard, lowTrump), Suit.SPADES));
        }

        @Test
        @DisplayName("Following (Losing): Void in lead, NO trump -> Dump lowest card")
        void followLosingVoidNoTrump() {
            Card lowDiscard = new Card(Suit.DIAMONDS, Rank.TWO);
            when(mockMemory.isLeadPlayer()).thenReturn(false);
            when(mockMemory.isTeamWinning(botId)).thenReturn(false);
            when(mockMemory.getCurrentTrump()).thenReturn(Suit.HEARTS);

            // Lead is SPADES. Hand has Diamonds & Clubs. Void in lead, void in trump.
            assertEquals(lowDiscard, strategy.chooseCardToPlay(List.of(new Card(Suit.CLUBS, Rank.NINE), lowDiscard), Suit.SPADES));
        }

        @Test
        @DisplayName("Following (Losing): Must follow suit -> Plays highest to try and win")
        void followLosingMustFollowSuit() {
            Card highLead = new Card(Suit.SPADES, Rank.KING);
            Card lowLead = new Card(Suit.SPADES, Rank.TWO);

            when(mockMemory.isLeadPlayer()).thenReturn(false);
            when(mockMemory.isTeamWinning(botId)).thenReturn(false);
            when(mockMemory.getCurrentTrump()).thenReturn(Suit.HEARTS);

            // Must follow SPADES
            assertEquals(highLead, strategy.chooseCardToPlay(List.of(highLead, lowLead), Suit.SPADES));
        }
    }

    @Nested
    @DisplayName("Play Phase Tactics: MISERIE Mode")
    class MiserieTacticTests {

        @BeforeEach
        void setupMiserie() {
            BidTurn miserieBid = new BidTurn(botId, BidType.MISERIE);
            lenient().when(mockMemory.getHighestBid()).thenReturn(miserieBid);
        }

        @Test
        @DisplayName("Leading: Plays absolute lowest card to avoid taking trick")
        void leadMiserie() {
            Card low = new Card(Suit.HEARTS, Rank.TWO);
            when(mockMemory.isLeadPlayer()).thenReturn(true);

            assertEquals(low, strategy.chooseCardToPlay(List.of(new Card(Suit.HEARTS, Rank.KING), low), null));
        }

        @Test
        @DisplayName("Following: Plays highest safe card that stays under the winner")
        void followMiserieSafe() {
            Card safeHigh = new Card(Suit.HEARTS, Rank.NINE);
            Card unsafeHigh = new Card(Suit.HEARTS, Rank.KING); // Beats the Ten

            when(mockMemory.isLeadPlayer()).thenReturn(false);
            when(mockMemory.getCurrentWinningTurn()).thenReturn(new PlayTurn(new PlayerId(), new Card(Suit.HEARTS, Rank.TEN)));

            // Plays 9 to stay under the 10, instead of dumping the 2.
            assertEquals(safeHigh, strategy.chooseCardToPlay(List.of(new Card(Suit.HEARTS, Rank.TWO), safeHigh, unsafeHigh), Suit.HEARTS));
        }

        @Test
        @DisplayName("Following: Plays absolute highest card if forced to win (no safe cards)")
        void followMiserieNoSafe() {
            Card absoluteHighest = new Card(Suit.HEARTS, Rank.ACE);
            Card otherHigh = new Card(Suit.HEARTS, Rank.QUEEN);

            when(mockMemory.isLeadPlayer()).thenReturn(false);
            when(mockMemory.getCurrentWinningTurn()).thenReturn(new PlayTurn(new PlayerId(), new Card(Suit.HEARTS, Rank.TWO)));

            // Everything beats a 2, so it must dump the absolute highest card to minimize future risk
            assertEquals(absoluteHighest, strategy.chooseCardToPlay(List.of(otherHigh, absoluteHighest), Suit.HEARTS));
        }
    }

    @Nested
    @DisplayName("Play Phase Tactics: ANTI-MISERIE Mode")
    class AntiMiserieTacticTests {

        private final PlayerId opponentId = new PlayerId();

        @BeforeEach
        void setupAntiMiserie() {
            BidTurn miserieBid = new BidTurn(opponentId, BidType.MISERIE);
            lenient().when(mockMemory.getHighestBid()).thenReturn(miserieBid);
        }

        @Test
        @DisplayName("Fallback: Plays lowest card if Miserie player hasn't acted yet")
        void miserieHasNotActed() {
            Card low = new Card(Suit.HEARTS, Rank.TWO);
            when(mockMemory.hasPlayerActedInCurrentTrick(opponentId)).thenReturn(false);

            assertEquals(low, strategy.chooseCardToPlay(List.of(new Card(Suit.HEARTS, Rank.ACE), low), Suit.HEARTS));
        }

        @Test
        @DisplayName("Opponent Winning: Plays lowest safe card to keep opponent winning")
        void miserieWinningHasSafe() {
            Card safeLow = new Card(Suit.HEARTS, Rank.THREE);
            Card winningCard = new Card(Suit.HEARTS, Rank.TEN);

            when(mockMemory.hasPlayerActedInCurrentTrick(opponentId)).thenReturn(true);
            when(mockMemory.calculateCurrentWinnerId()).thenReturn(opponentId);
            when(mockMemory.getCardPlayedBy(opponentId)).thenReturn(winningCard);

            // 3 is under 10. Ace beats 10. Play the 3 to force opponent to keep the trick.
            assertEquals(safeLow, strategy.chooseCardToPlay(List.of(new Card(Suit.HEARTS, Rank.ACE), safeLow), Suit.HEARTS));
        }

        @Test
        @DisplayName("Opponent Winning: Plays highest card if forced to overtake opponent")
        void miserieWinningNoSafe() {
            Card absoluteHighest = new Card(Suit.HEARTS, Rank.ACE);
            Card winningCard = new Card(Suit.HEARTS, Rank.TWO); // Opponent played a 2

            when(mockMemory.hasPlayerActedInCurrentTrick(opponentId)).thenReturn(true);
            when(mockMemory.calculateCurrentWinnerId()).thenReturn(opponentId);
            when(mockMemory.getCardPlayedBy(opponentId)).thenReturn(winningCard);

            // No cards are under 2. Bot MUST win. Dump the highest possible card.
            assertEquals(absoluteHighest, strategy.chooseCardToPlay(List.of(absoluteHighest, new Card(Suit.HEARTS, Rank.THREE)), Suit.HEARTS));
        }

        @Test
        @DisplayName("Opponent Losing (Safe): Plays highest card to win trick and seize control")
        void miserieLosing() {
            Card highest = new Card(Suit.HEARTS, Rank.ACE);

            when(mockMemory.hasPlayerActedInCurrentTrick(opponentId)).thenReturn(true);
            when(mockMemory.calculateCurrentWinnerId()).thenReturn(new PlayerId()); // Someone ELSE is winning

            // Opponent is currently safe. Bot plays aggressively to take control.
            assertEquals(highest, strategy.chooseCardToPlay(List.of(highest, new Card(Suit.HEARTS, Rank.TWO)), Suit.HEARTS));
        }
    }
}