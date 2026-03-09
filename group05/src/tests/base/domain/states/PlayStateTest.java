package base.domain.states;

import base.domain.WhistGame;
import base.domain.card.Suit;
import base.domain.player.HighBotStrategy;
import base.domain.player.HumanStrategy;
import base.domain.player.LowBotStrategy;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.trick.Trick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayStateTest {
    State playState;
    WhistGame game;
    Round currentRound;
    Trick currentTrick;
    List<Player> players = List.of(
            new Player(new HumanStrategy(), "P1"),
            new Player(new LowBotStrategy(), "L-BOT1"),
            new Player(new LowBotStrategy(), "L-BOT2"),
            new Player(new HighBotStrategy(), "H-BOT3")
    );

    @BeforeEach
    void setUp() {
        game = new WhistGame();
        for  (Player player : players) {
            game.addPlayer(player);
        }

        currentRound = new Round(players, players.getFirst(), 1); // NOT added yet to game... + getFirst should be P1
        currentTrick = null; // No trick played just yet when first instantiating

        assertThrows(IllegalStateException.class, () -> new PlayState(game)); // No round added to game
        game.addRound(currentRound); // add the round
        playState = new PlayState(game); // Instantiate playState

    }

    @Test
    void constructorTest() {
        assertEquals(game, playState.getGame()); // After a round has been added to a game

        assertNull(playState.getGame().getCurrentRound().getLastPlayedTrick());
        assertEquals(currentRound, playState.getGame().getCurrentRound());
        assertEquals(currentTrick, playState.getGame().getCurrentRound().getLastPlayedTrick());

        assertEquals(players, playState.getGame().getPlayers());

        assertEquals(1, playState.getGame().getRounds().size());
        assertNull(playState.getGame().getCurrentRound().getTrumpSuit());
    }


    @Test
    void executeState() {



    }

    @Test
    void nextState() {
    }
}