// base/domain/states/CountState.java
package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.results.*;
import base.domain.round.Round;
import base.storage.snapshots.SaveMode;

import java.util.List;

import static base.domain.bid.BidType.*;

/**
 * Manual score-counting flow (use case 1).
 * <p>
 * IO-agnostic: the adapter owns persistence. When the user picks "save" this
 * state emits a {@link SaveDescriptionResult} and, once the follow-up
 * {@link TextCommand} arrives, simply resumes the scoreboard.
 *
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class CountState extends State {

    private enum CountPhase {
        START, SELECT_BID, SELECT_TRUMP, SELECT_PLAYERS, SELECT_WINNERS,
        CALCULATE, PROMPT_NEXT_STATE, SAVE_DESCRIPTION
    }

    private CountPhase currentPhase = CountPhase.START;
    private int keuze;
    private Bid bid;
    private BidType selectedBidType;
    private Suit trumpSuit;
    private List<Player> participatingPlayers;

    public CountState(WhistGame game) {
        super(game);
    }

    @Override
    public StateStep executeState() {
        if (currentPhase == CountPhase.START) {
            currentPhase = CountPhase.SELECT_BID;
            return StateStep.stay(new BidSelectionResult(values(), getGame().getPlayers()));
        }
        return StateStep.stay(nextStep());
    }

    @Override
    public StateStep executeState(GameCommand command) {
        if (currentPhase == CountPhase.START) {
            currentPhase = CountPhase.SELECT_BID;
            return StateStep.stay(new BidSelectionResult(values(), getGame().getPlayers()));
        }

        return switch (command) {
            case BidCommand b        -> StateStep.stay(handleBidType(b.bid()));
            case SuitCommand s       -> StateStep.stay(handleSuit(s.suit()));
            case PlayerListCommand p -> StateStep.stay(handlePlayerInput(p.players()));
            case NumberCommand n     -> handleNumberInput(n.choice());
            // Adapter has already performed the save — just resume the scoreboard.
            case TextCommand ignored -> {
                currentPhase = CountPhase.PROMPT_NEXT_STATE;
                yield StateStep.stay(getScoreBoard());
            }
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
    }

    private StateStep handleNumberInput(int value) {
        if (currentPhase == CountPhase.CALCULATE) {
            return StateStep.stay(finalizeCalculation(value, null));
        }
        // PROMPT_NEXT_STATE
        if (value == 3) {
            currentPhase = CountPhase.SAVE_DESCRIPTION;
            return StateStep.stay(new SaveDescriptionResult(SaveMode.COUNT));
        }
        this.keuze = value;
        return StateStep.transitionWithoutResult();
    }

    private GameResult nextStep() {
        currentPhase = CountPhase.SELECT_BID;
        return new BidSelectionResult(values(), getGame().getPlayers());
    }

    private GameResult handleBidType(BidType type) {
        this.selectedBidType = type;
        if (type == MISERIE || type == OPEN_MISERIE) {
            currentPhase = CountPhase.SELECT_PLAYERS;
            return new PlayerSelectionResult(getGame().getPlayers(), true);
        }
        currentPhase = CountPhase.SELECT_TRUMP;
        return new SuitSelectionResult();
    }

    private GameResult handleSuit(Suit suit) {
        this.trumpSuit = suit;
        currentPhase = CountPhase.SELECT_PLAYERS;
        return new PlayerSelectionResult(getGame().getPlayers(), false);
    }

    private GameResult handlePlayerInput(List<Player> players) {
        if (currentPhase == CountPhase.SELECT_PLAYERS) {
            if (players == null || players.isEmpty()) {
                boolean multiSelect = (selectedBidType == MISERIE || selectedBidType == OPEN_MISERIE);
                return new PlayerSelectionResult(getGame().getPlayers(), multiSelect);
            }

            this.participatingPlayers = players;
            this.bid = selectedBidType.instantiate(participatingPlayers.getFirst(), trumpSuit);

            if (selectedBidType == MISERIE || selectedBidType == OPEN_MISERIE) {
                currentPhase = CountPhase.SELECT_WINNERS;
                return new PlayerSelectionResult(getGame().getPlayers(), true);
            }
            currentPhase = CountPhase.CALCULATE;
            return new AmountOfTrickWonResult();
        }
        // SELECT_WINNERS — participatingPlayers already set
        return finalizeCalculation(0, players);
    }

    private GameResult finalizeCalculation(int tricks, List<Player> winners) {
        Player primaryBidder = participatingPlayers.getFirst();
        Round round = new Round(getGame().getPlayers(), primaryBidder, 1);
        round.setHighestBid(bid);
        getGame().addRound(round);

        round.calculateScoresForCount(bid, tricks, participatingPlayers, winners);

        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        return getScoreBoard();
    }

    private GameResult getScoreBoard() {
        List<Integer> scores = getGame().getPlayers().stream().map(Player::getScore).toList();
        return new ScoreBoardResult(getPlayerNames(), scores);
    }

    private List<String> getPlayerNames() {
        return getGame().getPlayers().stream().map(Player::getName).toList();
    }

    @Override
    public State nextState() {
        if (keuze == 1) return new CountState(getGame());
        return null;
    }
}