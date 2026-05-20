package base.commands;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Maintains undo/redo stacks for reversibleAction instances.
 * Executing an action pushes it onto the undo stack and clears the redo stack.
 * Undoing pops from the undo stack and pushes onto the redo stack, and vice versa.
 */
public class ActionHistory {
    private final Deque<ReversibleAction> undoStack = new ArrayDeque<>();
    private final Deque<ReversibleAction> redoStack = new ArrayDeque<>();

    /**
     * Executes the given action, adds it to the undo stack, and clears the redo stack.
     *
     * @param action the action to execute; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     */
    public void execute(ReversibleAction action) {
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }
        action.execute();
        undoStack.push(action);
        redoStack.clear();
    }

    /** Undoes the most recent action and moves it to the redo stack.
     *
     * @throws IllegalStateException if there is nothing to undo
     */
    public void undo() {
        if (undoStack.isEmpty()) throw new IllegalStateException("Nothing to undo.");
        ReversibleAction action = undoStack.pop();
        action.undo();
        redoStack.push(action);
    }

    /** Re-applies the most recently undone action and moves it back to the undo stack.
     *
     * @throws IllegalStateException if there is nothing to redo
     */
    public void redo() {
        if (redoStack.isEmpty()) throw new IllegalStateException("Nothing to redo.");
        ReversibleAction action = redoStack.pop();
        action.execute();
        undoStack.push(action);
    }

    /** @return {@code true} if there is at least one action that can be undone */
    public boolean canUndo() { return !undoStack.isEmpty(); }

    /** @return {@code true} if there is at least one action that can be redone */
    public boolean canRedo() { return !redoStack.isEmpty(); }

    /** Clears both the undo and redo stacks. */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}