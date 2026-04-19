package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.*;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.commands.GameCommand;
import base.domain.commands.GameCommand.*;
import base.domain.player.PlayerId;
import base.domain.results.*;
import base.domain.bid.*;
import base.domain.round.Round;
import base.domain.results.CountResults.*;
import base.domain.results.PlayResults.*;
import java.util.List;

import static base.domain.bid.BidType.*;

/**
 * State responsible for manual score calculation (use case 1).
 * <p>
 * Pure domain logic: bid selection → trump → participants → (winners) → score.
 * Cross-cutting edit actions (save, add/remove player, remove round) are handled
 * by the IO layer via {@code CountSetupFlow} after each count.
 *
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class CountState extends State {

    private enum CountPhase {
        START, SELECT_BID, SELECT_TRUMP, SELECT_PLAYERS, SELECT_WINNERS,
        CALCULATE, PROMPT_NEXT_STATE
    }

    private CountPhase currentPhase = CountPhase.START;
    private int nextStateDecision;
    private Bid bid;
    private BidType selectedBidType;
    private Suit trumpSuit;
    private List<PlayerId> participatingPlayerIds;

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
            case BidCommand b -> StateStep.stay(handleBidType(b.bid()));
            case SuitCommand s -> StateStep.stay(handleSuit(s.suit()));
            case PlayerListCommand p -> handlePlayerInput(p.playerIds()); // now returns StateStep directly
            case NumberCommand n -> handleNumberInput(n.choice());
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
    }

    private StateStep handleNumberInput(int value) {
        if (currentPhase == CountPhase.CALCULATE) {
            return finalizeCalculation(value, null); // now returns StateStep directly
        }

        // PROMPT_NEXT_STATE — user picks continue (1) or quit (2).
        // All other post-round actions (save, add player, remove round...) live in the IO flow.
        return switch (value) {
            case 1 -> {
                nextStateDecision = 1;
                yield StateStep.transitionWithoutResult();
            }
            case 2 -> {
                nextStateDecision = 2;
                yield StateStep.transitionWithoutResult();
            }
            default -> throw new IllegalStateException("Unexpected number input: " + value);
        };
    }

    private GameResult nextStep() {
        currentPhase = CountPhase.SELECT_BID;
        return new BidSelectionResult(values(), getGame().getPlayers());
    }

    private GameResult handleBidType(BidType type) {
        this.selectedBidType = type;
        if (type == MISERIE || type == OPEN_MISERIE) {
            currentPhase = CountPhase.SELECT_PLAYERS;
            return new PlayerSelectionResult(getGame().getPlayers(), true, type);
        }
        currentPhase = CountPhase.SELECT_TRUMP;
        return new SuitSelectionResult();
    }

    private GameResult handleSuit(Suit suit) {
        this.trumpSuit = suit;
        currentPhase = CountPhase.SELECT_PLAYERS;
        if (selectedBidType == PROPOSAL || selectedBidType == TROEL || selectedBidType == TROELA) {
            return new PlayerSelectionResult(getGame().getPlayers(), true, selectedBidType);
        }
        return new PlayerSelectionResult(getGame().getPlayers(), false, selectedBidType);
    }

    private StateStep handlePlayerInput(List<PlayerId> players) {
        if (currentPhase == CountPhase.SELECT_PLAYERS) {
            if (players == null || players.isEmpty()) {
                boolean multiSelect = (selectedBidType == MISERIE || selectedBidType == OPEN_MISERIE
                        || selectedBidType == PROPOSAL || selectedBidType == TROEL || selectedBidType == TROELA);
                return StateStep.stay(new PlayerSelectionResult(getGame().getPlayers(), multiSelect, selectedBidType));
            }
            this.participatingPlayerIds = players;
            this.bid = selectedBidType.instantiate(participatingPlayerIds.getFirst(), trumpSuit);

            if (selectedBidType == MISERIE || selectedBidType == OPEN_MISERIE) {
                currentPhase = CountPhase.SELECT_WINNERS;
                return StateStep.stay(new PlayerSelectionResult(getGame().getPlayers(), true, bid.getType()));
            }
            currentPhase = CountPhase.CALCULATE;
            return StateStep.stay(new AmountOfTrickWonResult());
        }
        // SELECT_WINNERS
        return finalizeCalculation(0, players);
    }

    private StateStep finalizeCalculation(int tricks, List<PlayerId> winnersId) {
        Player primaryBidder = getGame().getPlayerById(participatingPlayerIds.getFirst());
        Round round = new Round(getGame().getPlayers(), primaryBidder, 1);
        round.setHighestBid(bid);
        getGame().addRound(round);

        List<Player> participatingPlayers = participatingPlayerIds.stream()
                .map(getGame()::getPlayerById)
                .toList();

        List<Player> winners = winnersId == null ? List.of()
                : winnersId.stream().map(getGame()::getPlayerById).toList();

        round.calculateScoresForCount(bid, tricks, participatingPlayers, winners);

        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        return StateStep.transitionWithoutResult(); // ScoreBoardFlow owns the scoreboard display
    }


    @Override
    public State nextState() {
        if (nextStateDecision == 1) return new CountState(getGame());
        return null;
    }
}