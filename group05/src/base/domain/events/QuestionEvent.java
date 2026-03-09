package base.domain.events;

public record QuestionEvent(String string) implements GameEvent {

    @Override
    public boolean isInputRequired() {
        return true;
    }
}