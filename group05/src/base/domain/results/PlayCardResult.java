package base.domain.results;

import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

public record PlayCardResult(
        List<Card> tableCards,
        boolean isOpenMiserie,
        List<String> exposedPlayerNames,
        List<List<Card>> formattedExposedHand,
        int trickNumber,
        Player player,
        List<Card> legalCards,
        Trick lastPlayedTrick
) implements GameResult {

    public PlayCardResult {
        if (tableCards == null) {
            throw new IllegalArgumentException("tableCards cannot be null");
        }
        if (exposedPlayerNames == null) {
            throw new IllegalArgumentException("exposedPlayerNames cannot be null");
        }
        if (formattedExposedHand == null) {
            throw new IllegalArgumentException("formattedExposedHand cannot be null");
        }
        if (trickNumber <= 0) {
            throw new IllegalArgumentException("trickNumber must be positive");
        }
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        if (legalCards == null || legalCards.isEmpty()) {
            throw new IllegalArgumentException("legalCards cannot be null or empty");
        }

        tableCards = List.copyOf(tableCards);
        exposedPlayerNames = List.copyOf(exposedPlayerNames);
        formattedExposedHand = formattedExposedHand.stream()
                .map(List::copyOf)
                .toList();
        legalCards = List.copyOf(legalCards);
    }
}
