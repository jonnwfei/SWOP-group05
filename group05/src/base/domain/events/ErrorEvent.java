package base.domain.events;

public record ErrorEvent(int lowerBound, int upperBound) implements GameEvent {

}
