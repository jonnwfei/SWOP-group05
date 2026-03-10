package base.domain.events.countEvents;

import base.domain.events.GameEvent;
import base.domain.player.Player;

import java.util.List;

public record MiserieWinnerEvent(List<Player> players) implements GameEvent {
    private String renderMiserieWinnerEvent(){
        return "Which players won their bid? (Got 0 tricks): \n" + printNames();
    }

    public String printNames() {
        String result = "Players in this game:\n";
        for (int i = 0; i < players.size(); i++) {
            result += (i + 1) + ". " + players.get(i).getName() + "\n";
        }
        return result;
    }
}
