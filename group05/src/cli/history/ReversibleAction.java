package cli.history;

public interface ReversibleAction {
    void execute();
    void undo();
}
