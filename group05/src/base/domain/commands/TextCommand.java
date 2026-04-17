package base.domain.commands;

public record TextCommand(String text) implements GameCommand {
    public TextCommand(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text cannot be null or blank");
        }
        this.text = text;
    }
}

