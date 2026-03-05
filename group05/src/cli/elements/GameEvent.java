package cli.elements;
public abstract class GameEvent {
    private final String content;

    public GameEvent(String content) {

        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public abstract boolean isInputRequired();
}