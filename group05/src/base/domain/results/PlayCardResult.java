package base.domain.results;

import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.trick.Trick;
import base.domain.turn.PlayTurn;

import java.util.List;
import java.util.Objects;

public record PlayCardResult(
        List<PlayTurn> turns,
        boolean isOpenMiserie,
        List<String> exposedPlayerNames,
        List<List<Card>> formattedExposedHands,
        int trickNumber,
        Player player,
        List<Card> legalCards,
        Trick lastPlayedTrick
) implements GameResult {

    public PlayCardResult {
        if (turns == null || turns.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("tableCards cannot be null or contain null objects");
        }
        if (exposedPlayerNames == null || exposedPlayerNames.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("exposedPlayerNames cannot be null or contain null objects");
        }
        if (formattedExposedHands == null || formattedExposedHands.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("formattedExposedHands cannot be null or contain null hands");
        }
        if (formattedExposedHands.stream().anyMatch(hand -> hand.stream().anyMatch(Objects::isNull))) {
            throw new IllegalArgumentException("formattedExposedHands cannot contain hands that contain null");
        }
        if (trickNumber <= 0) {
            throw new IllegalArgumentException("trickNumber must be positive");
        }
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        if (legalCards == null || legalCards.isEmpty() ||  legalCards.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("legalCards cannot be null or empty or contain null objects");
        }

        turns = List.copyOf(turns);
        exposedPlayerNames = List.copyOf(exposedPlayerNames);
        formattedExposedHands = formattedExposedHands.stream()
                .map(List::copyOf)
                .toList();
        legalCards = List.copyOf(legalCards);
    }
}
