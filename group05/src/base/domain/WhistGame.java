package base.domain;
import java.util.ArrayList;
import java.util.List;

import base.domain.deck.Deck;
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
    private Deck deck;

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
    /**
     * Gets the active round of the Game
     *
     * @return round being played
     * */
    public Round getCurrentRound(){
        return this.rounds.getLast();
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
     * Adds given round to this Game, rounds
     *
     * @param round to add
     */
    public void addRound (Round round){
        this.rounds.add(round);
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
    /**
     * Returns a formatted string of the players and their current score
     * */
    public String printScore(){
        String result = "======= SCORES =======\n";
        for (Player p : players) {
            result += p.getName() + ": " + p.getScore() + " points\n";
        }
        result += "======================";
        return result;
    }
}
