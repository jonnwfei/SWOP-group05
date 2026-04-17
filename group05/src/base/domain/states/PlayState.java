package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.CardMath;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.results.*;
import base.domain.round.Round;
import base.domain.trick.Trick;
import base.domain.turn.PlayTurn;
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

    /**
     * Initializes the PlayState and sets up the first trick.
     * * @param game The current game instance.
     *
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
        if (!currentPlayer.getRequiresConfirmation()) { // TODO: fix with polymorph ish shit?
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
            case PlayCardResult p -> buildNeedCardResult(currentPlayer); // TODO: should use parameter p?
            // All other meaningful results (EndOfTrickResult, TrickHistoryResult, etc.)
            default -> result;
        };

        return toStep(viewResult);
    }

    /**
     * Handles the player turn
     *
     * @param player The current human player
     * @param card card chosen
     * @return The resulting GameResult
     */
    private GameResult handleCardPlay(Player player, Card card) {
        // 1. Defensively check legality (though Adapter should prevent this)
        if (!CardMath.getLegalCards(player.getHand(), currentTrick.getLeadingSuit()).contains(card)) {
            throw new IllegalArgumentException("Card chosen is not legal!");
        }

        // 2. Play the card into the trick
        currentTrick.addTurn(player.getId(), card);

        // 3. BROADCAST: Notify all GameObservers (like SmartBotMemory) that a card was played!
        getGame().notifyTurnPlayed(new PlayTurn(player.getId(), card));

        // TODO: this is a bit hacky, we should probably return the winning player from processTurnOutcome instead of re-checking here
        boolean trickFinished = currentTrick.isCompleted();
        String winnerName = trickFinished ? getGame().getPlayerById(currentTrick.getWinningPlayerId()).getName() : null;

        if (trickFinished) {
            currentRound.registerCompletedTrick(currentTrick);

            if (currentRound.isFinished()) {
                return new EndOfRoundResult(player.getName(), card);
            }

            this.currentTrick = new Trick(currentTrick.getWinningPlayerId(), currentRound.getTrumpSuit());
            return new EndOfTrickResult(player.getName(), card, winnerName);
        } else {
            currentRound.advanceToNextPlayer();
            return new EndOfTurnResult(player.getName(), card);
        }
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

        Suit leadSuit = currentTrick.getLeadingSuit();
        List<Card> hand = player.getHand();
        List<Card> legalCards = CardMath.getLegalCards(hand, leadSuit);

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

            // Extension 11a: Check if a Miserie bidder won this trick
            if (currentRound.getHighestBid().getType() == BidType.MISERIE &&
                    currentRound.getBiddingTeamPlayers().contains(winningPlayer)) {
                roundOver = true;
                currentRound.signalEarlyFinish(); // This updates scores and flags the round as done
                return;
            }

            if (currentRound.getTricks().size() >= Round.MAX_TRICKS) {
                roundOver = true;
            } else {
                // Only start a new trick if the round isn't over
                this.currentTrick = new Trick(winningPlayer, currentRound.getTrumpSuit());
            }
        } else {
            currentRound.advanceToNextPlayer();
        }
    }

    /**
     * Determines the next state based on whether all tricks have been played.
     * * @return ScoreBoardState if the round is finished, otherwise remains in
     * PlayState.
     */
    @Override
    public State nextState() {
        // CRITICAL: Use the same logic that processTurnOutcome uses.
        // If roundOver is true, we MUST move to ScoreBoardState.
        if (roundOver || currentRound.isFinished()) {
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