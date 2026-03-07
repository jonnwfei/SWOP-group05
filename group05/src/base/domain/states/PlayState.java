package base.domain.states;

import base.GameController;
import base.domain.WhistGame;

import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.trick.Trick;
import cli.elements.GameEvent;
import cli.elements.QuestionEvent;
import cli.elements.TextEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class PlayState extends State {
    private Round currentRound;
    private Trick currentTrick;
    private int cardsPlayedInTrick;


    public PlayState(WhistGame game) {
        super(game);
        this.currentRound = game.getRounds().get(0);
        this.currentTrick = new Trick(currentRound.getCurrentPlayer(), currentRound.getTrumpSuit());
        this.cardsPlayedInTrick = 0;
    }

    @Override
    public GameEvent executeState(String input) {
        Player currentPlayer = currentRound.getCurrentPlayer();

        if (input != null && !input.isEmpty() && currentPlayer.getRequiresConfirmation()) {
            int handIdx = Integer.parseInt(input);
            Card playedCard = currentPlayer.getHand().get(handIdx); // TODO: indien input == playerResponse parse card input, may

            try {
                currentTrick.playCard(currentPlayer, playedCard);
                cardsPlayedInTrick++;
                currentRound.advanceToNextPlayer(); // TODO: implement advance to next Player in Round.
                return new TextEvent(currentPlayer + "played " + playedCard);
            } catch (IllegalArgumentException e) {
                return new QuestionEvent("Invalid move (" + e.getMessage() + "). Try again.");
            }
        } else if (!currentPlayer.getRequiresConfirmation()) {
            Card botCard = currentPlayer.chooseCard(currentRound.getTrumpSuit());
            currentTrick.playCard(currentPlayer, botCard);
            cardsPlayedInTrick++;
            currentRound.advanceToNextPlayer();

            return new TextEvent(currentPlayer + "played " + botCard.toString());
        }

        Player nextPlayer = currentRound.getCurrentPlayer();
        if (nextPlayer.getRequiresConfirmation()) {
            String currentHand = nextPlayer.getHand().toString();
            return new QuestionEvent("Trick: " + (currentRound.getTricks().size() + 1) +
                    " | " + nextPlayer.getName() + "'s turn.\n" +
                    "Your hand: " + currentHand + "\nChoose Card via index:");
        } else {
            return new TextEvent("");
        }
    }

    @Override
    public State nextState() {
        if (currentRound.getTricks().size() >= Round.MAX_TRICKS) {
            return new MenuState(getGame());
        }
        return this;
    }
}