package base.domain.events.menuEvents;

import base.domain.events.GameEvent;
import base.domain.player.Player;

import java.util.List;

public record PrintNamesEvent(List<Player> players) implements GameEvent {
    private String renderPrintNamesEvent(){
        return printNames();
    }
    public String printNames() {
        String result = "Players in this game:\n";
        for (int i = 0; i < players.size(); i++) {
            result += (i + 1) + ". " + players.get(i).getName() + "\n";
        }
        return result;
    }
}
