package base.domain.results;

import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

public record PlayCardResult(    // e.g., ["Player1 played ACE of SPADES", ...]
                                 List<Card> tableCards,      // e.g., ["Player1 played ACE of SPADES", ...]
                                 boolean isOpenMiserie,         // Tells the UI whether to draw the exposed hand
                                 List<String> exposedPlayerNames,      // Who is playing Open Miserie
                                 List<List<Card>> formattedExposedHand,   // The actual cards to show everyone
                                 int trickNumber,
                                 Player player,
                                 List<Card> legalCards, Trick lastPlayedTrick) implements  GameResult{
}
