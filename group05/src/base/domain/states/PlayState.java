package base.domain.states;

import base.domain.WhistGame;

import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.events.playevents.*;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.trick.Trick;
import base.domain.trick.Turn;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class PlayState extends State {
    private final Round currentRound;
    private Trick currentTrick;
    private boolean roundOver = false;

    // We use an internal phase machine to cleanly handle the human device-passing flow!
    private enum TurnPhase {
        PROMPT_PLAYER, REVEAL_HAND, WAIT_FOR_INPUT
    }
    private TurnPhase currentPhase = TurnPhase.PROMPT_PLAYER;

    /**
     * Instantiates the playState.
     *
     * @param game the given game that holds this State
     */
    public PlayState(WhistGame game) {
        super(game);
        Round round = game.getCurrentRound();
        if (round == null) {
            throw new IllegalStateException("Cannot create PlayState: no currentRound exist.");
        }
        this.currentRound = round;
        this.currentTrick = new Trick(currentRound.getCurrentPlayer(), currentRound.getTrumpSuit());
    }

    /**
     * Handles the execution of playState, orchestrating the logic flow of a Round.
     */
    @Override
    public GameEvent executeState(GameAction action) {
        Player currentPlayer = currentRound.getCurrentPlayer();

        // 1. Process BOT Turn
        if (!currentPlayer.getRequiresConfirmation()) {
            Card botCard = currentPlayer.chooseCard(currentTrick.getLeadingSuit());
            currentTrick.playCard(currentPlayer, botCard);

            // Capture state before processing
            boolean trickFinished = currentTrick.isCompleted();
            String trickWinner = trickFinished ? currentTrick.getWinningPlayer().getName() : null;

            processTurnOutcome(); // Resets the trick

            if (roundOver) {
                return new EndOfRoundEvent(currentPlayer.getName(), botCard);
            } else if (trickFinished) {
                return new EndOfTrickEvent(currentPlayer.getName(), botCard, trickWinner);
            } else {
                return new EndOfTurnEvent(currentPlayer.getName(), botCard);
            }
        }

        // 2. Process HUMAN Turn via Phase Machine
        return switch (currentPhase) {
            case PROMPT_PLAYER  -> handlePromptPlayer(currentPlayer);
            case REVEAL_HAND    -> handleRevealHand(currentPlayer);
            case WAIT_FOR_INPUT -> handleWaitForInput(currentPlayer, action);
        };
    }

    // --- PHASE HANDLERS ---
    private GameEvent handlePromptPlayer(Player currentPlayer) {
        currentPhase = TurnPhase.REVEAL_HAND;
        return new InitiateTurnEvent(currentPlayer.getName());
    }

    private GameEvent handleRevealHand(Player currentPlayer) {
        // We received the ContinueAction from pressing Enter, now we show the hand
        currentPhase = TurnPhase.WAIT_FOR_INPUT;
        return buildPickCardEvent(currentPlayer);
    }

    private GameEvent handleWaitForInput(Player currentPlayer, GameAction action) {
        // Enforce NumberAction
        if (!(action instanceof NumberAction numAction)) {
            return new ErrorEvent(0, currentPlayer.getHand().size());
        }

        int choice = numAction.value();
        int maxChoice = currentPlayer.getHand().size();

        // Bounds check
        if (choice < 0 || choice > maxChoice) {
            return new ErrorEvent(0, maxChoice);
        }

        // Handle "View Last Trick"
        if (choice == 0) {
            Trick lastTrick = getGame().getCurrentRound().getLastPlayedTrick();
            if (lastTrick == null) {
                return new ErrorEvent(1, maxChoice); // 0 is invalid if no last trick exists
            } else {

                return new LastTrickEvent(lastTrick);
            }
        }

        // Attempt to play the card
        Card playedCard = currentPlayer.getHand().get(choice - 1);
        try {
            currentTrick.playCard(currentPlayer, playedCard);
        } catch (IllegalArgumentException e) {
            // Domain rule violation (e.g., must follow suit)
            //TODO : seperate error message, this is just asking for the same event to fix the problem
            return buildPickCardEvent(currentPlayer);
        }

        // --- NEW LOGIC: Capture state BEFORE processing the outcome ---
        boolean trickFinished = currentTrick.isCompleted();
        String trickWinner = trickFinished ? currentTrick.getWinningPlayer().getName() : null;

        // Success! Reset phase for the next player and process outcome
        currentPhase = TurnPhase.PROMPT_PLAYER;
        processTurnOutcome(); // This resets this.currentTrick

        if (roundOver) {
            return new EndOfRoundEvent(currentPlayer.getName(), playedCard);
        } else if (trickFinished) {
            // Use the saved variables here!
            return new EndOfTrickEvent(currentPlayer.getName(), playedCard, trickWinner);
        } else {
            return new EndOfTurnEvent(currentPlayer.getName(), playedCard);
        }
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
     * Helper method to construct the PickCardEvent by extracting and formatting
     * all necessary data from the current Domain state.
     */
    private PickCardEvent buildPickCardEvent(Player currentPlayer) {
        // 1. Handle Open Miserie Logic
        boolean isOpenMiserie = currentRound.getHighestBid() != null &&
                currentRound.getHighestBid().getType() == BidType.OPEN_MISERIE;

        String exposedName = "";
        List<Card> exposedHand = new ArrayList<>();

        if (isOpenMiserie) {
            Player proposer = currentRound.getHighestBid().getPlayer();
            exposedName = proposer.getName();
            exposedHand = proposer.getHand();
        }

        // 2. Map Cards to Strings
        List<Card> tableCards = currentTrick.getTurns().stream().map(Turn::playedCard).toList();

        List<Card> playerHand = currentPlayer.getHand();
        // 3. Instantiate and return the event
        return new PickCardEvent(
                tableCards,
                isOpenMiserie,
                exposedName,
                exposedHand,
                currentRound.getTricks().size() + 1,
                currentPlayer.getName(),
                playerHand
        );
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
            return new ScoreBoardState(this.getGame(), ScoreBoardState.RestartTarget.BID_STATE);
        }
        return this;
    }
}