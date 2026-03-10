package base.domain.events.countEvents;

import base.domain.events.GameEvent;

import java.util.List;

public record PlayersInBidEvent(List<String> playerNames) implements GameEvent {
    public PlayersInBidEvent {
        playerNames = List.copyOf(playerNames); // Defensive copy
    }
}