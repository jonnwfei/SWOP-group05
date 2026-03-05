package cli.elements;

public class TextElement extends GameEvent {
    public TextElement(String content) {
        super(content);
    }

    @Override
    public boolean isInputRequired() {
        return false;
    }
}