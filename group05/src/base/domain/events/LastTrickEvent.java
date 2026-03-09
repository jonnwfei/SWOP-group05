package base.domain.events;

import base.domain.trick.Trick;
import base.domain.trick.Turn;

public record LastTrickEvent(Trick trick) implements GameEvent {
    private String handleLastTrickEvent(){
        StringBuilder table = new StringBuilder("\n-------------- LAST PLAYED TRICK ---------------\n");

        for (Turn turn : trick.getTurns()) {
            table.append("- ").append(turn.toString()).append("\n");
        }
        table.append("------------------------------------------------\n");
        return table.toString();
    }
}

