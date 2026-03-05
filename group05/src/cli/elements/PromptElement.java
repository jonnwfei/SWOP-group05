package cli.elements;

public class PromptElement extends GameEvent {
    public PromptElement(String content) {
        super(content);
    }

    @Override
    public boolean isInputRequired() {
        return true;
    }
}