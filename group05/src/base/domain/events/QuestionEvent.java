package base.domain.events;

public class QuestionEvent extends GameEvent {
    public QuestionEvent(String content) {
        super(content);
    }

    @Override
    public boolean isInputRequired() {
        return true;
    }
}