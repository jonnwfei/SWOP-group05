package base.domain.results;

import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.trick.Trick;
import base.domain.turn.PlayTurn;

import java.util.List;
import java.util.Objects;

public sealed interface PlayResults extends GameResult {

    record PlayCardResult(
            List<PlayTurn> turns,
            boolean isOpenMiserie,
            List<String> exposedPlayerNames,
            List<List<Card>> formattedExposedHand,
            int trickNumber,
            Player player,
            List<Card> legalCards,
            Trick lastPlayedTrick
    ) implements PlayResults {
        public PlayCardResult {
            if (turns == null || turns.stream().anyMatch(Objects::isNull))
                throw new IllegalArgumentException("turns cannot be nor contain null.");
            if (exposedPlayerNames == null || exposedPlayerNames.stream().anyMatch(Objects::isNull))
                throw new IllegalArgumentException("exposedPlayerNames cannot be nor contain null.");
            if (formattedExposedHand == null ||  formattedExposedHand.stream().anyMatch(Objects::isNull))
                throw new IllegalArgumentException("formattedExposedHand cannot be nor contain null Hands.");
            if (formattedExposedHand.stream().anyMatch(hand -> hand.stream().anyMatch(Objects::isNull)))
                throw new IllegalArgumentException("formattedExposedHand cannot contain null cards.");
            if (exposedPlayerNames.size() != formattedExposedHand.size())
                throw new IllegalArgumentException("exposedPlayerNames and formattedExposedHand must have the same size.");
            if (trickNumber < 1)
                throw new IllegalArgumentException("trickNumber must be at least 1.");
            if (player == null)
                throw new IllegalArgumentException("player cannot be null.");
            if (legalCards == null)
                throw new IllegalArgumentException("legalCards cannot be null.");
            // lastPlayedTrick is intentionally nullable (null = no trick played yet)
            turns = List.copyOf(turns);
            exposedPlayerNames = List.copyOf(exposedPlayerNames);
            formattedExposedHand = formattedExposedHand.stream()
                    .map(List::copyOf)
                    .toList();
            legalCards = List.copyOf(legalCards);
        }
    }

    record EndOfTurnResult(String name, Card card) implements PlayResults {
        public EndOfTurnResult {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("name cannot be null or blank.");
            if (card == null)
                throw new IllegalArgumentException("card cannot be null.");
        }
    }

    record EndOfTrickResult(String name, Card card, String winner) implements PlayResults {
        public EndOfTrickResult {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("name cannot be null or blank.");
            if (card == null)
                throw new IllegalArgumentException("card cannot be null.");
            if (winner == null || winner.isBlank())
                throw new IllegalArgumentException("winner cannot be null or blank.");
        }
    }

    record EndOfRoundResult(String name, Card card) implements PlayResults {
        public EndOfRoundResult {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("name cannot be null or blank.");
            if (card == null)
                throw new IllegalArgumentException("card cannot be null.");
        }
    }

    record TrickHistoryResult(Trick trick) implements PlayResults {
        public TrickHistoryResult {
            if (trick == null)
                throw new IllegalArgumentException("trick cannot be null.");
        }
    }

    record ParticipatingPlayersResult(List<String> playerNames) implements PlayResults {
        public ParticipatingPlayersResult {
            if (playerNames == null || playerNames.isEmpty())
                throw new IllegalArgumentException("playerNames cannot be null or empty.");
            playerNames = List.copyOf(playerNames);
        }
    }
}