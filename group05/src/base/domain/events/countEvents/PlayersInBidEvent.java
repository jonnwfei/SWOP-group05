package base.domain.events.countEvents;

import base.domain.events.GameEvent;
import base.domain.player.Player;

import java.util.List;

public record PlayersInBidEvent(List<Player> players) implements GameEvent {
    private String renderPlayersInBidEvent(){
        return "Which player numbers played this bid?\n" + printNames();

    }
    //expects list of integers
    // all between 1-4
    /**
     * Returns a formatted string of the players in this game as follows:
     * <pre>
     * Players in this game:
     * 1. player1
     * 2. player2
     * etc.
     * </pre>
     *
     * @return the formatted player names
     */
    public String printNames() {
        String result = "Players in this game:\n";
        for (int i = 0; i < players.size(); i++) {
            result += (i + 1) + ". " + players.get(i).getName() + "\n";
        }
        return result;
    }

}
/**
 *
 *
 * checks here
 *     if (participatingPlayers.isEmpty()) {
 *             return new QuestionEvent("Select at least one player:");
 *         }
 *         for (int idx : participatingPlayers) {
 *             if (idx < 0 || idx >= getGame().getPlayers().size()) {
 *                 return new QuestionEvent("Invalid player index: " + idx);
 *             }
 *         }
 *         if (numberBid == 2 && participatingPlayers.size() != 2) {
 *             return new QuestionEvent("Select exactly two players:\n" + getGame().printNames());
 *         }
 *         else if (numberBid != 2&&numberBid != 7 && numberBid !=8 && participatingPlayers.size() != 1){
 *             return new QuestionEvent("Select exactly one player:\n" + getGame().printNames());
 *         }
 */
