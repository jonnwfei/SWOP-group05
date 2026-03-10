package base.domain.events.countEvents;

import base.domain.events.GameEvent;
import base.domain.player.Player;

import java.util.List;

/**
 * A Data Transfer Object (DTO) containing the current standings of all players.
 * * This event is typically dispatched at the end of a Round or a manual Count
 * sequence. It provides the View with a synchronized list of names and their
 * corresponding total scores to be rendered as a leaderboard.
 *
 * @param playerNames An immutable list of player names in the game.
 * @param scores      An immutable list of integers representing the total
 * accumulated points for each player, ordered to match
 * the {@code playerNames} list.
 */
public record ScoreBoardEvent(List<String> playerNames, List<Integer> scores) implements GameEvent {
    public ScoreBoardEvent {
        playerNames = List.copyOf(playerNames);
        scores = List.copyOf(scores);
    }
}