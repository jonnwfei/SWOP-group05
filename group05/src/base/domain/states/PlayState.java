package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.bid.Bid;
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
 * Manages the active gameplay phase where players play cards to complete
 * tricks.
 *
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class PlayState extends State {
    private final Round currentRound;
    private Trick currentTrick;
    private boolean roundOver = false;

    /**
     * Defines the internal workflow
     */
    private enum TurnPhase {
        PROMPT_PLAYER, REVEAL_HAND, WAIT_FOR_INPUT
    }

    private TurnPhase currentPhase = TurnPhase.PROMPT_PLAYER;

    /**
     * Initializes the PlayState and sets up the first trick.
     * 
     * @param game The current game instance.
     * @throws IllegalStateException if no round is currently active.
     */
    public PlayState(WhistGame game) {
        super(game);
        Round round = game.getCurrentRound();
        if (round == null) {
            throw new IllegalStateException("Cannot create PlayState: no currentRound exists.");
        }
        this.currentRound = round;
        this.currentTrick = new Trick(currentRound.getCurrentPlayer(), currentRound.getTrumpSuit());
    }

    /**
     * Executes a single turn.
     * 
     * @param action The user action (typically a card selection or "Continue").
     * @return The event to be rendered by the UI.
     */
    @Override
    public GameEvent<?> executeState(GameAction action) {
        Player currentPlayer = currentRound.getCurrentPlayer();

        // 1. Process BOT Turn: Bots play immediately without phases
        if (!currentPlayer.getRequiresConfirmation()) {
            Card botCard = currentPlayer.chooseCard(currentTrick.getLeadingSuit());
            currentTrick.playCard(currentPlayer, botCard);

            boolean trickFinished = currentTrick.isCompleted();
            String trickWinner = trickFinished ? currentTrick.getWinningPlayer().getName() : null;

            processTurnOutcome();

            if (roundOver)
                return new EndOfRoundEvent(currentPlayer.getName(), botCard);
            if (trickFinished)
                return new EndOfTrickEvent(currentPlayer.getName(), botCard, trickWinner);
            return new EndOfTurnEvent(currentPlayer.getName(), botCard);
        }

        // 2. Process HUMAN Turn via internal Phase Machine
        return switch (currentPhase) {
            case PROMPT_PLAYER -> handlePromptPlayer(currentPlayer);
            case REVEAL_HAND -> handleRevealHand(currentPlayer);
            case WAIT_FOR_INPUT -> handleWaitForInput(currentPlayer, action);
        };
    }

    /**
     * Prepares the "Pass device" screen.
     * 
     * @return GameEvent
     */
    private GameEvent<?> handlePromptPlayer(Player currentPlayer) {
        currentPhase = TurnPhase.REVEAL_HAND;
        return new InitiateTurnEvent(currentPlayer.getName());
    }

    /**
     * Transitions to the card selection screen after user confirmation.
     * 
     * @return GameEvent
     */
    private GameEvent<?> handleRevealHand(Player currentPlayer) {
        currentPhase = TurnPhase.WAIT_FOR_INPUT;
        return buildPickCardEvent(currentPlayer);
    }

    /**
     * Validates the card choice, plays it, and checks for trick/round completion.
     * 
     * @return GameEvent
     */
    private GameEvent<?> handleWaitForInput(Player currentPlayer, GameAction action) {
        Integer choice = switch (action) {
            case NumberAction(int value) -> value;
            default -> null;
        };
        if (choice == null) {
            return new ErrorEvent(0, currentPlayer.getHand().size());
        }

        int maxChoice = currentPlayer.getHand().size();

        if (choice < 0 || choice > maxChoice)
            return new ErrorEvent(0, maxChoice);

        // Option [0]: View the previous trick
        if (choice == 0) {
            Trick lastTrick = getGame().getCurrentRound().getLastPlayedTrick();
            return (lastTrick == null) ? new ErrorEvent(1, maxChoice) : new LastTrickEvent(lastTrick);
        }

        Card playedCard = currentPlayer.getHand().get(choice - 1);
        try {
            currentTrick.playCard(currentPlayer, playedCard);
        } catch (IllegalArgumentException e) {
            return buildPickCardEvent(currentPlayer);
        }

        boolean trickFinished = currentTrick.isCompleted();
        String trickWinner = trickFinished ? currentTrick.getWinningPlayer().getName() : null;

        currentPhase = TurnPhase.PROMPT_PLAYER;
        processTurnOutcome();

        if (roundOver)
            return new EndOfRoundEvent(currentPlayer.getName(), playedCard);
        if (trickFinished)
            return new EndOfTrickEvent(currentPlayer.getName(), playedCard, trickWinner);
        return new EndOfTurnEvent(currentPlayer.getName(), playedCard);
    }

    /**
     * Updates the round state and initializes the next trick if the current one is
     * full.
     */
    private void processTurnOutcome() {
        if (currentTrick.isCompleted()) {
            Player winningPlayer = currentTrick.getWinningPlayer();
            currentRound.registerCompletedTrick(currentTrick);
            if (currentRound.getTricks().size() >= Round.MAX_TRICKS) {
                roundOver = true;
            }
            this.currentTrick = new Trick(winningPlayer, currentRound.getTrumpSuit());
        } else {
            currentRound.advanceToNextPlayer();
        }
    }

    /**
     * Data-mapping helper to create a PickCardEvent with all table and hand
     * details.
     * 
     * @return PickCardEvent
     */
    private PickCardEvent buildPickCardEvent(Player currentPlayer) {
        boolean isOpenMiserie = currentRound.getHighestBid() != null &&
                currentRound.getHighestBid().getType() == BidType.OPEN_MISERIE;

        List<String> exposedName = new ArrayList<>();
        List<List<Card>> exposedHand = new ArrayList<>();

        if (isOpenMiserie) {
            for (Bid bid : currentRound.getBids()) {
                if (bid.getType() == BidType.OPEN_MISERIE) {
                    Player proposer = bid.getPlayer();
                    exposedName.add(proposer.getName());
                    exposedHand.add(proposer.getHand());
                }
            }
        }

        List<Card> tableCards = currentTrick.getTurns().stream().map(Turn::playedCard).toList();

        return new PickCardEvent(
                tableCards,
                isOpenMiserie,
                exposedName,
                exposedHand,
                currentRound.getTricks().size() + 1,
                currentPlayer.getName(),
                currentPlayer.getHand());
    }

    /**
     * Determines the next state based on whether all tricks have been played.
     * 
     * @return ScoreBoardState if the round is finished, otherwise remains in
     *         PlayState.
     */
    @Override
    public State nextState() {
        if (currentRound.getTricks().size() >= Round.MAX_TRICKS) {
            return new ScoreBoardState(this.getGame());
        }
        return this;
    }
}