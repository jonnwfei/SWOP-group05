package base.domain.results;

import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

public record PlayCardResult(
        List<Card> tableCards,
        boolean isOpenMiserie,
        List<String> exposedPlayerNames,
        List<List<Card>> formattedExposedHands,
        int trickNumber,
        Player player,
        List<Card> legalCards,
        Trick lastPlayedTrick
) implements GameResult {

    public PlayCardResult {
        if (tableCards == null ||  tableCards.contains(null)) {
            throw new IllegalArgumentException("tableCards cannot be null or contain null objects");
        }
        if (exposedPlayerNames == null || exposedPlayerNames.contains(null)) {
            throw new IllegalArgumentException("exposedPlayerNames cannot be null or contain null objects");
        }
        if (formattedExposedHands == null || formattedExposedHands.contains(null)) {
            throw new IllegalArgumentException("formattedExposedHands cannot be null or contain null hands");
        }
        if (formattedExposedHands.stream().anyMatch(hand -> hand.contains(null))) {
            throw new IllegalArgumentException("formattedExposedHands cannot contain hands that contain null");
        }
        if (trickNumber <= 0) {
            throw new IllegalArgumentException("trickNumber must be positive");
        }
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        if (legalCards == null || legalCards.isEmpty() ||  legalCards.contains(null)) {
            throw new IllegalArgumentException("legalCards cannot be null or empty or contain null objects");
        }

        tableCards = List.copyOf(tableCards);
        exposedPlayerNames = List.copyOf(exposedPlayerNames);
        formattedExposedHands = formattedExposedHands.stream()
                .map(List::copyOf)
                .toList();
        legalCards = List.copyOf(legalCards);
    }
}
