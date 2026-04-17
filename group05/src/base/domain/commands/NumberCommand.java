package base.domain.commands;

public record NumberCommand(int choice) implements GameCommand {
    public NumberCommand(int choice) {
        if (choice < 1 || choice > 10) {
            throw new IllegalArgumentException("choice must be between 1 and 10");
        }
        this.choice = choice;
    }
}
