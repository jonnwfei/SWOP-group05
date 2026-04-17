package base.domain.commands;

public record TextCommand(String text) implements GameCommand {
    public TextCommand {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text cannot be null or blank");
        }
    }
}