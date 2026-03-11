package base.domain.actions;

import java.util.ArrayList;

public record NumberListAction(ArrayList<Integer> integers) implements GameAction {
}
