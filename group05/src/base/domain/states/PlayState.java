package base.domain.states;

import base.domain.WhistGame;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.trick.Trick;
import base.domain.trick.Turn;
import cli.elements.*;


/**
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class PlayState extends State {
    private final Round currentRound;
    private Trick currentTrick;
    private boolean isHandHidden;
    private boolean firstTurn = true;
    private boolean roundOver = false;

    /**
     * Instantiates the playState.
     *
     * @param game the given game that holds this State
     */
    public PlayState(WhistGame game) {
        super(game);
        Round round = game.getCurrentRound();
        if (round == null) {
            throw new IllegalStateException("Cannot create PlayState: no currentRound exist; make sure a round has been created.");
        }
        this.currentRound = round; // will not throw since BidState ensures that a new Round has been instantiated
        this.currentTrick = new Trick(currentRound.getCurrentPlayer(), currentRound.getTrumpSuit());
        this.isHandHidden = false;
    }

    /**
     * Handles the execution of playState, orchestrating the logic flow of a Round and its Tricks, in the given game
     * that holds this state.
     *
     *
     * @return the next GameEvent
     */
    @Override
    public GameEvent executeState(String input) {
        Player currentPlayer = currentRound.getCurrentPlayer();
        StringBuilder outputLog = new StringBuilder();

        // Initial Load
        if (input == null || firstTurn) {
            isHandHidden = true;
            firstTurn = false;
            return new InitiateTurnEvent(currentPlayer);

        }

        if (isHandHidden) {
            isHandHidden = false;

            return new PickCardEvent(currentRound, currentPlayer);
        }

        // Checks if the input for HUMAN Turn
        if (currentPlayer.getRequiresConfirmation()) {
            try {
                int handIdx = Integer.parseInt(input);
                if (handIdx < 0 || handIdx > currentPlayer.getHand().size()) {
                    return new ErrorEvent(0, currentPlayer.getHand().size());
                }
                if (handIdx == 0) {
                    Trick lastTrick = getGame().getCurrentRound().getLastPlayedTrick();
                    if (lastTrick == null){
                        return new ErrorEvent(1,currentPlayer.getHand().size());
                    }
                    else{
                        return new LastTrickEvent(lastTrick);
                    }
                }

                Card playedCard = currentPlayer.getHand().get(handIdx - 1);
                currentTrick.playCard(currentPlayer, playedCard); // plays the card and checks if the played card is valid, could throw
                //outputLog.append(currentPlayer.getName()).append(" played ").append(playedCard.toString()).append(".\n");

                isHandHidden = true; // Hide the hand for next player
                processTurnOutcome();
                if (roundOver){
                    return new EndOfRoundEvent(currentPlayer, playedCard);
                }
                else{
                    return new EndOfTrickEvent(currentPlayer, playedCard);
                }

            } catch (IllegalArgumentException e) {
                return new ErrorEvent(0, currentPlayer.getHand().size());
            }
        }
        // Updates the reference pointer if HUMAN just played
        currentPlayer = currentRound.getCurrentPlayer();

        // Loop as long as the next players are BOTS
        while (!currentPlayer.getRequiresConfirmation()) { // Else it's a BOT Turn
            Card botCard = currentPlayer.chooseCard(currentTrick.getLeadingSuit());
            currentTrick.playCard(currentPlayer, botCard);
            processTurnOutcome();
            if (roundOver) {
                return new EndOfRoundEvent(currentPlayer, botCard);
            }
            currentPlayer = currentRound.getCurrentPlayer(); // Updates the reference pointer to the next BOT
        }

        return new InitiateTurnEvent(currentPlayer);

    }

    /**
     * Helper method to process the outcome of a Turn. As it checks if a Trick or Round has ended.q
     *
     */
    private void processTurnOutcome() {
        if (currentTrick.isCompleted()) { // Check if TRICK is completed
            Player winningPlayer = currentTrick.getWinningPlayer();
            currentRound.registerCompletedTrick(currentTrick);
            if (currentRound.getTricks().size() >= Round.MAX_TRICKS) { // We check if Round has already completed 13 tricks
                roundOver = true;
            }
            this.currentTrick = new Trick(winningPlayer, currentRound.getTrumpSuit());
        } else {
            currentRound.advanceToNextPlayer();
        }
    }


    /**
     * Returns the nextState. After playState, the game either transitions to a new MenuState when the current Round has
     * finished, or returns this same playState instance so that the current round can continue.
     *
     * @return the next state
     */
    @Override
    public State nextState() {
        if (currentRound.getTricks().size() >= Round.MAX_TRICKS) {
            return new ScoreBoardState(this.getGame(), new BidState(getGame()));
        }
        return this;
    }
}