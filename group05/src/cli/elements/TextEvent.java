package cli.elements;

public class TextEvent extends GameEvent {
    public TextEvent(String content) {
        super(content);
    }

    @Override
    public boolean isInputRequired() {
        return false;
    }
}