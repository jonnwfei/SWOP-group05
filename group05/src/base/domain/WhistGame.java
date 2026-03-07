package base.domain;
import java.util.ArrayList;
import java.util.List;
import base.domain.player.*;
import base.domain.round.Round;
import base.domain.states.*;
import cli.elements.GameEvent;

/**
 * @author Stan Kestens
 * @since 28/02/2026
 */
public class WhistGame {

    private State state;
    private List<Player> players;
    private List<Round> rounds;
    private Player currentPlayer;
    private Player dealerPlayer;

    public WhistGame(){
        this.state = new MenuState(this);
        this.players = new ArrayList<>();
        this.rounds = new ArrayList<>();
        this.currentPlayer = null;
        this.dealerPlayer = null;
    }

    /**
     * Gets a shallow copy of the list of Players
     *
     * @return list of Player
     */
    public List<Player> getPlayers(){
        return new ArrayList<>(this.players);
    }

    /**
     * Gets a shallow copy of the list of Rounds
     *
     * @return list of Rounds
     */
    public List<Round> getRounds(){
        return new ArrayList<>(this.rounds);
    }

    /**
     * Gets the dealer of this Game
     *
     * @return dealerPlayer of this Game
     */
    public Player getDealerPlayer(){
        return this.dealerPlayer;
    }

    /**
     * Gets the active player of Game
     *
     * @return currentPlayer of Game
     */
    public Player getCurrentPlayer(){
        return this.currentPlayer;
    }


    public void nextState(){
        this.state = state.nextState();
    }

    public GameEvent executeState(String response){
        return state.executeState(response);
    }

    /**
     * Adds given player to this Game, players
     *
     * @param player to add
     */
    public void addPlayer(Player player){
        this.players.add(player);
    }

    /**
     * Returns a formatted string of the players in this game as follows:
     * <pre>
     * Players in this game:
     * - player1
     * - player2
     * etc.
     * </pre>
     *
     * @return the formatted player names
     */
    public String printNames() {
        String result = "Players in this game:\n";
        for (Player p : players) {
            result += "- " + p.getName() + "\n";
        }
        return result;
    }
}
