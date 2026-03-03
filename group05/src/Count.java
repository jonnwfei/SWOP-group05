
import group05.model.Player;
import group05.model.Card;
import group05.model.Bid;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/*
  @author Stan Kestens
  @since 23/02/2026
   */
public abstract class Count {

    private List<Player> players;
    private List<Round> rounds;

    /*
     * Constructs a game object
     *
     * */
    public Game() {
        this.players = new ArrayList<>();
        this.rounds = new ArrayList<>();
        this.currentPlayer = null;
    }

    // Getters
    /*
     * Gives the list of players
     *
     * @return the list of players
     * */
    public List<Player> getPlayers() {
        return players;
    }
    /*
     * Gives the list of rounds
     *
     * @return the list of rounds
     * */
    public List<Round> getRounds() {
        return rounds;
    }
    /*
     * Gives the currentPlayer
     *
     * @return the currentPlayer
     * */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    /*
     * Sets the current player
     *
     * @param new current player
     * @throws NullPointerException if the new player is null or if player is not from this game
     * */
    public void setCurrentPlayer(Player newCurrentPlayer) {
        if (!players.contains(newCurrentPlayer) || newCurrentPlayer == null) {
            this.currentPlayer = newCurrentPlayer;
        }
    }


}
