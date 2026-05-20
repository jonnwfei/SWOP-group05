package base.commands;

public interface ReversibleAction {
    void execute();
    void undo();
}