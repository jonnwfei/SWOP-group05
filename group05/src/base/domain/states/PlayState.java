package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.results.*;
import base.domain.round.Round;
import base.domain.trick.Trick;
import base.domain.trick.Turn;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
     * Initializes the PlayState and sets up the first trick.
     * * @param game The current game instance.
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
     * * @param command The user action (typically a card selection or "Continue").
     * @return The event to be rendered by the UI.
     */
    @Override
    public GameResult executeState(Optional<GameCommand> command) {

        if (roundOver) {
            return new EndOfRoundResult(currentRound.getCurrentPlayer().getName(), null);
        }

        Player currentPlayer = currentRound.getCurrentPlayer();

        // Handle incoming command
        if (command.isPresent()) {
            GameResult result = handlePlayerMove(currentPlayer, command.get());

            // If round ended → return immediately
            if (roundOver || result instanceof EndOfRoundResult) {
                return result;
            }

            // If it's a meaningful result (not just UI refresh)
            if (!(result instanceof PlayCardResult)) {
                return result;
            }
        }

        // Always return current state view
        return buildNeedCardResult(currentPlayer);
    }

    /**
     * Handles the player turn
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
                    currentTrick.playCard(player, card);
                } catch (IllegalArgumentException e) {
                    yield buildNeedCardResult(player);
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
                    Player proposer = bid.getPlayer();
                    exposedNames.add(proposer.getName());
                    exposedHands.add(proposer.getHand());
                }
            }
        }

        List<Card> tableCards = currentTrick.getTurns()
                .stream()
                .map(Turn::playedCard)
                .toList();

        List<Card> legalCards = currentTrick.getLegalCards(player);

        return new PlayCardResult(
                tableCards,
                isOpenMiserie,
                exposedNames,
                exposedHands,
                currentRound.getTricks().size() + 1,
                player,
                legalCards,
                currentRound.getLastPlayedTrick()
        );
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

        List<Card> tableCards = currentTrick.getTurns()
                .stream()
                .map(Turn::playedCard)
                .toList();

        List<Card> legalCards = currentTrick.getLegalCards(player);

        return new PlayCardResult(
                tableCards,
                isOpenMiserie,
                exposedNames,
                exposedHands,
                currentRound.getTricks().size() + 1,
                player,
                legalCards,
                currentRound.getLastPlayedTrick()
        );
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
}