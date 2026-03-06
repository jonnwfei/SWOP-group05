package base.domain;
import java.util.ArrayList;
import java.util.List;
import base.domain.player.*;
import base.domain.round.Round;
import base.domain.states.*;
import cli.elements.GameEvent;

public class WhistGame {

    private State state;
    private boolean running;
    private List<Player> players;
    private List<Round> rounds;
    private Player currentPlayer;
    private Player dealerPlayer;

    public WhistGame(){
        this.state = new MenuState(this);
        this.running = true;
        this.players = new ArrayList<>();
        this.rounds = new ArrayList<>();
        this.currentPlayer = null;
        this.dealerPlayer = null;
    }
    public List<Player> getPlayers(){
        return this.players;
    }

    public void nextState(){
        this.state = state.nextState();
    }
    public GameEvent executeState(String response){
        return state.executeState(response);
    }
    public void addPlayer(Player player){
        this.players.add(player);
    }
    public String printNames() {
        String result = "Players in this game:\n";
        for (Player p : players) {
            result += "- " + p.getName() + "\n";
        }
        return result;
    }
}
