package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.CardMath;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.results.*;
import base.domain.round.Round;
import base.domain.trick.Trick;
import base.domain.turn.PlayTurn;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the active gameplay phase where players play cards to complete tricks.
 *
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class PlayState extends State {
    private final Round currentRound;
    private Trick currentTrick;

    /**
     * Initializes the PlayState and sets up the first trick.
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
        this.currentTrick = new Trick(currentRound.getCurrentPlayer().getId(), currentRound.getTrumpSuit());
    }

    /**
     * Executes a single turn without a user command (e.g. when a bot is playing).
     *
     * @return The event to be rendered by the UI.
     */
    @Override
    public StateStep executeState() {
        if (currentRound.isFinished()) {
            return StateStep.transition(new EndOfRoundResult(currentRound.getCurrentPlayer().getName(), null));
        }

        Player currentPlayer = currentRound.getCurrentPlayer();
        if (!currentPlayer.getRequiresConfirmation()) {
            return toStep(handleBotTurn(currentPlayer));
        }

        // Always return current state view
        return StateStep.stay(buildNeedCardResult(currentPlayer));
    }

    /**
     * Executes a single turn based on a user command.
     *
     * @param command The user action (typically a card selection or a request to view the last trick)
     * @return The event to be rendered by the UI.
     */
    @Override
    public StateStep executeState(GameCommand command) {
        if (currentRound.isFinished()) {
            return StateStep.transition(new EndOfRoundResult(currentRound.getCurrentPlayer().getName(), null));
        }

        Player currentPlayer = currentRound.getCurrentPlayer();
        GameResult result = handlePlayerMove(currentPlayer, command);

        GameResult viewResult = switch (result) {
            // If the round ended during the move, return the result immediately
            case GameResult r when currentRound.isFinished() -> r;
            // Return an explicit EndOfRoundResult as-is
            case EndOfRoundResult e -> e;
            // If it's a PlayCardResult (UI refresh), intercept it and build the needed view
            case PlayCardResult p -> buildNeedCardResult(currentPlayer);
            // All other meaningful results (EndOfTrickResult, TrickHistoryResult, etc.)
            default -> result;
        };

        return toStep(viewResult);
    }

    /**
     * Handles the player turn
     *
     * @param player The current human player
     * @param command The command received from the UI
     * @return The resulting GameResult
     */
    private GameResult handlePlayerMove(Player player, GameCommand command) {
        return switch (command) {
            case NumberCommand n when n.choice() == 0 -> {
                Trick last = currentRound.getLastPlayedTrick();
                yield (last == null)
                        ? buildNeedCardResult(currentRound.getCurrentPlayer())
                        : new TrickHistoryResult(last);
            }

            case CardCommand c -> {
                Card card = c.card();

                if (!player.getHand().contains(card)) {
                    yield buildNeedCardResult(player);
                }

                try {
                    currentTrick.addTurn(player.getId(), card);
                    // FIX: Notify observers so Smart Bots can see the card!
                    getGame().notifyTurnPlayed(new PlayTurn(player.getId(), card));
                } catch (IllegalArgumentException e) {
                    yield buildNeedCardResult(player);
                }

                boolean trickFinished = currentTrick.isCompleted();

                // Note: Not hacky! We must extract this before processTurnOutcome resets the trick.
                String winner = null;
                if (trickFinished) {
                    winner = getGame().getPlayerById(currentTrick.getWinningPlayerId()).getName();
                }

                processTurnOutcome();

                if (currentRound.isFinished())
                    yield new EndOfRoundResult(player.getName(), card);

                if (trickFinished)
                    yield new EndOfTrickResult(player.getName(), card, winner);

                yield new EndOfTurnResult(player.getName(), card);
            }

            default -> buildNeedCardResult(player);
        };
    }

    private GameResult buildNeedCardResult(Player player) {
        boolean isOpenMiserie = currentRound.getHighestBid() != null &&
                currentRound.getHighestBid().getType() == BidType.OPEN_MISERIE;

        List<String> exposedNames = new ArrayList<>();
        List<List<Card>> exposedHands = new ArrayList<>();

        if (isOpenMiserie) {
            for (Bid bid : currentRound.getBids()) {
                if (bid.getType() == BidType.OPEN_MISERIE) {
                    Player proposer = getGame().getPlayerById(bid.getPlayerId());
                    exposedNames.add(proposer.getName());
                    exposedHands.add(proposer.getHand());
                }
            }
        }

        List<Card> tableCards = currentTrick.getTurns()
                .stream()
                .map(PlayTurn::playedCard)
                .toList();

        List<Card> legalCards = CardMath.getLegalCards(player.getHand(), currentTrick.getLeadingSuit());

        return new PlayCardResult(
                tableCards,
                isOpenMiserie,
                exposedNames,
                exposedHands,
                currentRound.getTricks().size() + 1,
                player,
                legalCards,
                currentRound.getLastPlayedTrick());
    }

    /**
     * Handle a bot turn
     */
    private GameResult handleBotTurn(Player player) {
        Card card = player.chooseCard(currentTrick.getLeadingSuit());
        currentTrick.addTurn(player.getId(), card);

        // FIX: Notify observers so other Smart Bots can see the card!
        getGame().notifyTurnPlayed(new PlayTurn(player.getId(), card));

        boolean trickFinished = currentTrick.isCompleted();
        String winner = null;
        if (trickFinished) {
            winner = getGame().getPlayerById(currentTrick.getWinningPlayerId()).getName();
        }

        processTurnOutcome();

        if (currentRound.isFinished())
            return new EndOfRoundResult(player.getName(), card);

        if (trickFinished)
            return new EndOfTrickResult(player.getName(), card, winner);

        return new EndOfTurnResult(player.getName(), card);
    }

    /**
     * Updates the round state and initializes the next trick if the current one is full.
     */
    private void processTurnOutcome() {
        if (currentTrick.isCompleted()) {
            Player winningPlayer = getGame().getPlayerById(currentTrick.getWinningPlayerId());

            // FIX: registerCompletedTrick internally checks for max tricks AND Miserie early termination!
            currentRound.registerCompletedTrick(currentTrick);

            if (!currentRound.isFinished()) {
                // FIX: Pass the PlayerId to the Trick constructor
                this.currentTrick = new Trick(winningPlayer.getId(), currentRound.getTrumpSuit());
            }
        } else {
            currentRound.advanceToNextPlayer();
        }
    }

    /**
     * Determines the next state based on whether all tricks have been played.
     */
    @Override
    public State nextState() {
        // Since Round is the Information Expert, we just ask it if it is done.
        if (currentRound.isFinished()) {
            return new ScoreBoardState(this.getGame());
        }
        return this;
    }

    private StateStep toStep(GameResult result) {
        return switch (result) {
            case EndOfRoundResult ignored -> StateStep.transition(result);
            default -> StateStep.stay(result);
        };
    }
}