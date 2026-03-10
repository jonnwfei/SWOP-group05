package base.domain.events.countEvents;

import base.domain.events.GameEvent;
import base.domain.player.Player;

import java.util.List;

public record ScoreBoardEvent(List<String> playerNames, List<Integer> scores) implements GameEvent {
    public ScoreBoardEvent {
        playerNames = List.copyOf(playerNames);
        scores = List.copyOf(scores);
    }
}