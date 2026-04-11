package base.domain.results;

import base.domain.card.Card;

import java.util.List;

public record PlayCardResult(    // e.g., ["Player1 played ACE of SPADES", ...]
                                 List<Card> cardsOnTable,      // e.g., ["Player1 played ACE of SPADES", ...]
                                 boolean isOpenMiserie,         // Tells the UI whether to draw the exposed hand
                                 List<String> exposedPlayerName,      // Who is playing Open Miserie
                                 List<List<Card>> formattedExposedHand,   // The actual cards to show everyone
                                 int trickNumber,
                                 String currentPlayerName,
                                 List<Card> currentPlayerHand ) implements  GameResult{
}
