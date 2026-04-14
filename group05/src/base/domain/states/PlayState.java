package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.commands.*;
import base.domain.commands.GameCommand;
import base.domain.commands.NumberCommand;
import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.events.playevents.*;
import base.domain.player.Player;
import base.domain.results.GameResult;
import base.domain.results.TrickInputResult;
import base.domain.round.Round;
import base.domain.trick.Trick;
import base.domain.turn.PlayTurn;
import java.util.ArrayList;
import java.util.List;
import base.domain.results.*;

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
        PROMPT_PLAYER, REVEAL_HAND
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
     * @param command The user action (typically a card selection or "Continue").
     * @return The event to be rendered by the UI.
     */
    @Override
    public GameResult executeState(GameCommand command) {
        Player currentPlayer = currentRound.getCurrentPlayer();

        //  BOT TURN
        if (!currentPlayer.getRequiresConfirmation()) {
            return handleBotTurn(currentPlayer);
        }

        //  HUMAN TURN
        if (command == null) {
            // No input yet → just tell UI what to render
            return buildPickCardResult(currentPlayer);
        }

        // Input received → process immediately
        return handlePlayerMove(currentPlayer, command);
    }

    /**
     * Handles the player turn
     * @param player
     * @param command
     * @return
     */
    private GameResult handlePlayerMove(Player player, GameCommand command) {

        return switch (command) {
            case NumberCommand n when n.choice() == 0 -> {
                Trick last = currentRound.getLastPlayedTrick();
                yield (last == null)
                        ? buildPickCardResult(currentRound.getCurrentPlayer())
                        : new TrickHistoryResult(last);
            }

            case CardCommand c -> {
                Card card = c.card();

                if (!player.getHand().contains(card)) {
                    yield buildPickCardResult(player);
                }

                try {
                    currentTrick.playCard(player, card);
                } catch (IllegalArgumentException e) {
                    yield buildPickCardResult(player);
                }

                boolean trickFinished = currentTrick.isCompleted();
                String winner = trickFinished ? currentTrick.getWinningPlayer().getName() : null;

                processTurnOutcome();

                if (roundOver)
                    yield new EndOfRoundResult(player.getName(), card);

                if (trickFinished)
                    yield new EndOfTrickResult(player.getName(), card,  winner);

                yield new EndOfTurnResult(player.getName(), card);
            }

            default -> buildPickCardResult(player);
        };
    }

    private GameResult buildPickCardResult(Player player) {
        boolean isOpenMiserie = currentRound.getHighestBid() != null &&
                currentRound.getHighestBid().getType() == BidType.OPEN_MISERIE;

        List<String> exposedNames = new ArrayList<>();
        List<List<Card>> exposedHands = new ArrayList<>();

        if (isOpenMiserie) {
            for (Bid bid : currentRound.getBids()) {
                if (bid.getType() == BidType.OPEN_MISERIE) {
                    Player proposer = bid.getPlayer();
                    exposedNames.add(proposer.getName());
                    exposedHands.add(proposer.getHand());
                }
            }
        }

        List<Card> tableCards = currentTrick.getTurns().stream().map(PlayTurn::playedCard).toList();

        return new PlayCardResult(
                tableCards,
                isOpenMiserie,
                exposedNames,
                exposedHands,
                currentRound.getTricks().size() + 1,
                player.getName(),
                player.getHand());
    }


    /**
     * Handle a bot turn
     */
    private GameResult handleBotTurn(Player player) {
        Card card = player.chooseCard(currentTrick.getLeadingSuit());
        currentTrick.playCard(player, card);

        boolean trickFinished = currentTrick.isCompleted();
        String winner = trickFinished ? currentTrick.getWinningPlayer().getName() : null;

        processTurnOutcome();

        if (roundOver)
            return new EndOfRoundResult(player.getName(), card);

        if (trickFinished)
            return new EndOfTrickResult(player.getName(), card, winner);

        return new EndOfTurnResult(player.getName(), card);
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