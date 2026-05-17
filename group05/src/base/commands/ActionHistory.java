package base.commands;

import java.util.ArrayDeque;
import java.util.Deque;

public class ActionHistory {
    private final Deque<ReversibleAction> undoStack = new ArrayDeque<>();
    private final Deque<ReversibleAction> redoStack = new ArrayDeque<>();

    public void execute(ReversibleAction action) {
        action.execute();
        undoStack.push(action);
        redoStack.clear();
    }

    public void undo() {
        if (undoStack.isEmpty()) throw new IllegalStateException("Nothing to undo.");
        ReversibleAction action = undoStack.pop();
        action.undo();
        redoStack.push(action);
    }

    public void redo() {
        if (redoStack.isEmpty()) throw new IllegalStateException("Nothing to redo.");
        ReversibleAction action = redoStack.pop();
        action.execute();
        undoStack.push(action);
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}