package base.domain.actions;

import java.util.ArrayList;

/** * Multi-selection input (e.g., players in a bid or Miserie winners).
 * @param integers List of selected indices, usually from comma-separated input.
 * @author Tommy
 * @since 10/03/2026
 * */
public record NumberListAction(ArrayList<Integer> integers) implements GameAction {
}
