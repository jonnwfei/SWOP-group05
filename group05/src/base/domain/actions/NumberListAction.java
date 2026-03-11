package base.domain.actions;

import java.util.ArrayList;

/** * Multi-selection input (e.g., players in a bid or Miserie winners).
 * @param integers List of selected indices, usually from comma-separated input.
 */
public record NumberListAction(ArrayList<Integer> integers) implements GameAction {
}
