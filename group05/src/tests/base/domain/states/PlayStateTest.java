package base.domain.states;

import base.domain.WhistGame;
import base.domain.card.Suit;
import base.domain.player.HighBotStrategy;
import base.domain.player.HumanStrategy;
import base.domain.player.LowBotStrategy;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.trick.Trick;
import cli.elements.GameEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
//
class PlayStateTest {
    GameEvent gameEvent;
    PlayState playState;
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

    /**
     * Currently not in works yet, this could change after refactoring of using new GameEvents
     */
    @Test
    void executeState() {
//        Player currentPlayer = playState.getGame().getCurrentRound().getCurrentPlayer();
//
//        // FIRST INITIAL PROMPT
//        gameEvent = playState.executeState("firstTurn, dus dit maakt nie uit");
//        assertTrue(gameEvent.isInputRequired());
//        String expectedOutput = "\n============== Pass the terminal to " + currentPlayer.getName() + " ==============\n" + "Press ANY BUTTON to reveal your hand...";
//        assertEquals(expectedOutput, gameEvent.getContent());
//
//        // SECOND PROMPT, showHand
//        gameEvent = playState.executeState("dit maakt ook nie uit");
//        assertTrue(gameEvent.isInputRequired());
//
//        String expectedOutput2 = "\nTrick: " + (currentRound.getTricks().size() + 1) +
//                " | " + currentPlayer.getName() + "'s turn.\n" + "(0) to show last played Trick.\n" +
//                "Your hand: \n" + currentPlayer.getFormattedHand() + "\nChoose Card via index:";
//        assertTrue(gameEvent.getContent().contains(expectedOutput2));
//
//        // THIRD PROMPT aka first TURN → HUMAN P1 TURN
//        gameEvent = playState.executeState("abcd"); // FOR ERROR CATCH
//        assertTrue(gameEvent.isInputRequired());
//        assertEquals("Invalid hand number\nChoose (0) to see the last trick or between 1 and " + currentPlayer
//                .getHand().size() + ":", gameEvent.getContent()); // THIS looks at the catch
//        gameEvent = playState.executeState("3000"); // FOR invalid input CATCH
//        assertTrue(gameEvent.isInputRequired());
//        assertEquals("Invalid hand number\nChoose (0) to see the last trick or between 1 and " + currentPlayer
//                .getHand().size() + ": ", gameEvent.getContent()); // THIS looks at the catch
//
//        //
//        gameEvent = playState.executeState("0");
//        assertTrue(gameEvent.isInputRequired());
//        assertEquals("No last played trick has been found.\n" + "\nChoose Card via index:", gameEvent.getContent()); // FOR some reason is the string trimmed?
//


    }

    /**
     * Currently not in works yet, this could change after refactoring of using new GameEvents
     */
    @Test
    void nextState() {
        State nextState = playState.nextState();
        assertInstanceOf(PlayState.class, nextState);
        assertEquals(playState.getGame().getRounds().size(), nextState.getGame().getRounds().size());

        Trick completedTrick = new Trick(currentRound.getCurrentPlayer(), Suit.HEARTS);
//        while(completedTrick.getTurns().size() < Trick.MAX_TURNS) {
//            completedTrick.playCard(currentRound.getCurrentPlayer(), new Card(Suit.HEARTS, Rank.ACE));
//        }
//
//
//        while(playState.getGame().getRounds().size() < Round.MAX_TRICKS) {
//            playState.getGame().getCurrentRound().registerCompletedTrick(completedTrick);
//        }
//        System.out.println(playState.getGame().getCurrentRound().getTricks().size());
//
//        nextState = playState.nextState();
//        assertInstanceOf(ScoreBoardState.class, nextState);


    }
}