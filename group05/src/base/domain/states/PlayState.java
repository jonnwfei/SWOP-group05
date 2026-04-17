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
 */
public class PlayState extends State {
    private final Round currentRound;
    private Trick currentTrick;

    public PlayState(WhistGame game) {
        super(game);
        Round round = game.getCurrentRound();
        if (round == null) {
            throw new IllegalStateException("Cannot create PlayState: no currentRound exists.");
        }
        this.currentRound = round;
        this.currentTrick = new Trick(currentRound.getCurrentPlayer().getId(), currentRound.getTrumpSuit());
    }

    @Override
    public StateStep executeState() {
        if (currentRound.isFinished()) {
            return StateStep.transition(new EndOfRoundResult(currentRound.getCurrentPlayer().getName(), null));
        }

        Player currentPlayer = currentRound.getCurrentPlayer();
        if (!currentPlayer.getRequiresConfirmation()) {
            return toStep(handleBotTurn(currentPlayer));
        }

        return StateStep.stay(buildNeedCardResult(currentPlayer));
    }

    @Override
    public StateStep executeState(GameCommand command) {
        if (currentRound.isFinished()) {
            return StateStep.transition(new EndOfRoundResult(currentRound.getCurrentPlayer().getName(), null));
        }

        Player currentPlayer = currentRound.getCurrentPlayer();
        GameResult result = handlePlayerMove(currentPlayer, command);

        // CLEANUP: Greatly simplified. If the result is a prompt for a card, rebuild the fresh UI.
        // Otherwise, just return the meaningful result (EndOfTrick, TrickHistory, etc.)
        if (result instanceof PlayCardResult) {
            return toStep(buildNeedCardResult(currentPlayer));
        }

        return toStep(result);
    }

    private GameResult handlePlayerMove(Player player, GameCommand command) {
        return switch (command) {
            case NumberCommand n when n.choice() == 0 -> {
                Trick last = currentRound.getLastPlayedTrick();
                yield (last == null) ? buildNeedCardResult(player) : new TrickHistoryResult(last);
            }
            case CardCommand c -> {
                Card card = c.card();
                List<Card> legalCards = CardMath.getLegalCards(player.getHand(), currentTrick.getLeadingSuit());

                // CLEANUP: Validate explicitly instead of using try/catch for flow control
                if (!legalCards.contains(card)) {
                    yield buildNeedCardResult(player);
                }

                yield executeCardPlay(player, card);
            }
            default -> buildNeedCardResult(player);
        };
    }

    private GameResult handleBotTurn(Player player) {
        Card card = player.chooseCard(currentTrick.getLeadingSuit());
        return executeCardPlay(player, card);
    }

    /**
     * CLEANUP: Extracted the massive duplication between bots and humans into a single method.
     * Both actors play cards the exact same way.
     */
    private GameResult executeCardPlay(Player player, Card card) {
        // 1. Play card, remove from hand, and broadcast
        currentTrick.addTurn(player.getId(), card);
        player.removeCard(card);
        getGame().notifyTurnPlayed(new PlayTurn(player.getId(), card));

        // 2. Pre-calculate winner before processTurnOutcome clears the trick
        boolean trickFinished = currentTrick.isCompleted();
        String winner = trickFinished ? getGame().getPlayerById(currentTrick.getWinningPlayerId()).getName() : null;

        // 3. Let the round handle the physics
        processTurnOutcome();

        // 4. Return the correct progression event
        if (currentRound.isFinished()) return new EndOfRoundResult(player.getName(), card);
        if (trickFinished) return new EndOfTrickResult(player.getName(), card, winner);
        return new EndOfTurnResult(player.getName(), card);
    }

    private void processTurnOutcome() {
        if (currentTrick.isCompleted()) {
            Player winningPlayer = getGame().getPlayerById(currentTrick.getWinningPlayerId());

            // Round registers the trick and automatically scores itself if it's the 13th or all miserie players won
            currentRound.finalizeTrick(currentTrick);

            this.currentTrick = new Trick(winningPlayer.getId(), currentRound.getTrumpSuit());
        } else {
            currentRound.advanceToNextPlayer();
        }
    }

    private GameResult buildNeedCardResult(Player player) {
        boolean isOpenMiserie = currentRound.getHighestBid() != null &&
                currentRound.getHighestBid().getType() == BidType.OPEN_MISERIE;

        List<String> exposedNames = new ArrayList<>();
        List<List<Card>> exposedHands = new ArrayList<>();

        // CLEANUP: Helper method call to keep this builder clean and readable
        if (isOpenMiserie) {
            populateExposedHands(exposedNames, exposedHands);
        }

        List<PlayTurn> turns = currentTrick.getTurns();
        List<Card> legalCards = CardMath.getLegalCards(player.getHand(), currentTrick.getLeadingSuit());

        return new PlayCardResult(
                turns, isOpenMiserie, exposedNames, exposedHands,
                currentRound.getTricks().size() + 1, player, legalCards,
                currentRound.getLastPlayedTrick()
        );
    }

    /**
     * Helper method to isolate the Open Miserie visibility logic
     */
    private void populateExposedHands(List<String> exposedNames, List<List<Card>> exposedHands) {
        for (Bid bid : currentRound.getBids()) {
            if (bid.getType() == BidType.OPEN_MISERIE) {
                Player proposer = getGame().getPlayerById(bid.getPlayerId());
                exposedNames.add(proposer.getName());
                exposedHands.add(proposer.getHand());
            }
        }
    }

    /**
     * Determines the next state based on whether all tricks have been played.
     * * @return ScoreBoardState if the round is finished, otherwise remains in
     * PlayState.
     */
    @Override
    public State nextState() {
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