package base.domain.commands;

public record NumberCommand(int choice) implements GameCommand {
    public NumberCommand {
        if (choice < 0) {
            throw new IllegalArgumentException("choice must be positive");
        }
    }
}