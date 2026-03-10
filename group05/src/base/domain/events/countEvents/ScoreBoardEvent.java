package base.domain.events.countEvents;

import base.domain.events.GameEvent;
import base.domain.player.Player;

import java.util.List;

public record ScoreBoardEvent(List<Player> players) implements GameEvent {
    private String renderScoreBoardEvent(){
       return printScore() + "\n" +
                "Do you want to: \n(1) Simulate another round\n(2) Go back to the main menu";
    }
    /**
     * Returns a formatted string of the players and their current score
     * */
    public String printScore(){
        String result = "============== SCORES ==============\n";
        for (Player p : players) {
            result += p.getName() + ": " + p.getScore() + " points\n";
        }
        result += "====================================";
        return result;
    }
}
