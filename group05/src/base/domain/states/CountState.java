package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.*;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.results.*;
import base.domain.round.Round;
import base.domain.strategy.HumanStrategy;

import java.util.ArrayList;
import java.util.List;

import static base.domain.bid.BidType.*;

/**
 * State responsible for manual score calculation (use case 1).
 * <p>
 * IO-agnostic: the adapter owns persistence. When the user picks "save" this
 * state emits a {@link CountSaveDescriptionResult} and, once the follow-up
 * {@link TextCommand} arrives, simply returns to the scoreboard.
 *
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class CountState extends State {

    private enum CountPhase {
        START, SELECT_BID, SELECT_TRUMP, SELECT_PLAYERS, SELECT_WINNERS,
        CALCULATE, PROMPT_NEXT_STATE, SAVE_DESCRIPTION,
        ADD_PLAYER, REMOVE_PLAYER_SELECT, REMOVE_ROUND
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
            case PlayerListCommand p -> {
                if (currentPhase == CountPhase.REMOVE_PLAYER_SELECT) {
                    yield StateStep.stay(handleRemovePlayer(p.playerIds()));
                }
                yield StateStep.stay(handlePlayerInput(p.playerIds()));
            }
            case NumberCommand n -> handleNumberInput(n.choice());
            case TextCommand t -> {
                if (currentPhase == CountPhase.SAVE_DESCRIPTION) {
                    // Adapter has already persisted; just resume the scoreboard.
                    currentPhase = CountPhase.PROMPT_NEXT_STATE;
                    yield StateStep.stay(getScoreBoard());
                }
                if (currentPhase == CountPhase.ADD_PLAYER) {
                    yield StateStep.stay(handleAddPlayer(t.text()));
                }
                throw new IllegalStateException("Unexpected text input in phase: " + currentPhase);
            }
            case RoundCommand r -> StateStep.stay(handleRound(r.round()));
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
    }

    private StateStep handleNumberInput(int value) {
        if (currentPhase == CountPhase.CALCULATE) {
            return StateStep.stay(finalizeCalculation(value, null));
        }

        if (currentPhase == CountPhase.REMOVE_ROUND && value == 0) {
            currentPhase = CountPhase.PROMPT_NEXT_STATE;
            return StateStep.stay(getScoreBoard());
        }

        return switch (value) {
            case 1 -> {
                nextStateDecision = 1;
                yield StateStep.transitionWithoutResult();
            }
            case 2 -> {
                nextStateDecision = 2;
                yield StateStep.transitionWithoutResult();
            }
            case 3 -> {
                currentPhase = CountPhase.SAVE_DESCRIPTION;
                yield StateStep.stay(new CountSaveDescriptionResult());
            }
            case 4 -> {
                currentPhase = CountPhase.REMOVE_ROUND;
                yield StateStep.stay(new DeleteRoundResult(getGame().getRounds()));
            }
            case 5 -> {
                currentPhase = CountPhase.ADD_PLAYER;
                yield StateStep.stay(new AddHumanPlayerResult());
            }
            case 6 -> {
                currentPhase = CountPhase.REMOVE_PLAYER_SELECT;
                yield StateStep.stay(new PlayerSelectionResult(getGame().getPlayers()));
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

    private GameResult handlePlayerInput(List<PlayerId> players) {
        if (currentPhase == CountPhase.SELECT_PLAYERS) {
            if (players == null || players.isEmpty()) {
                boolean multiSelect = (selectedBidType == MISERIE || selectedBidType == OPEN_MISERIE
                        || selectedBidType == PROPOSAL || selectedBidType == TROEL || selectedBidType == TROELA);
                return new PlayerSelectionResult(getGame().getPlayers(), multiSelect, selectedBidType);
            }

            this.participatingPlayerIds = players;
            this.bid = selectedBidType.instantiate(participatingPlayerIds.getFirst(), trumpSuit);

            if (selectedBidType == MISERIE || selectedBidType == OPEN_MISERIE) {
                currentPhase = CountPhase.SELECT_WINNERS;
                return new PlayerSelectionResult(getGame().getPlayers(), true, bid.getType());
            }
            currentPhase = CountPhase.CALCULATE;
            return new AmountOfTrickWonResult();
        }
        return finalizeCalculation(0, players);
    }

    private GameResult finalizeCalculation(int tricks, List<PlayerId> winnersId) {
        Player primaryBidder = getGame().getPlayerById(participatingPlayerIds.getFirst());
        Round round = new Round(getGame().getPlayers(), primaryBidder, 1);
        round.setHighestBid(bid);
        getGame().addRound(round);

        List<Player> participatingPlayers = participatingPlayerIds.stream()
                .map(getGame()::getPlayerById)
                .toList();

        List<Player> winners = new ArrayList<>();
        if (winnersId != null) {
            winners = winnersId.stream().map(getGame()::getPlayerById).toList();
        }

        round.calculateScoresForCount(bid, tricks, participatingPlayers, winners);

        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        return getScoreBoard();
    }

    private GameResult getScoreBoard() {
        List<Integer> scores = getGame().getPlayers().stream().map(Player::getScore).toList();
        boolean canRemove = canRemovePlayer();
        return new ScoreBoardResult(getPlayerNames(), scores, canRemove);
    }

    private GameResult handleAddPlayer(String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isBlank()) {
            return new AddHumanPlayerResult();
        }
        getGame().addPlayer(new Player(new HumanStrategy(), cleanName));
        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        return getScoreBoard();
    }

    private GameResult handleRemovePlayer(List<PlayerId> playerIds) {
        if (!canRemovePlayer()) {
            currentPhase = CountPhase.PROMPT_NEXT_STATE;
            return getScoreBoard();
        }
        if (playerIds.isEmpty()) {
            return new PlayerSelectionResult(getGame().getPlayers());
        }
        Player newPlayer = getGame().getPlayerById(playerIds.getFirst());
        getGame().removePlayer(newPlayer);
        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        return getScoreBoard();
    }

    private List<String> getPlayerNames() {
        return getGame().getPlayers().stream().map(Player::getName).toList();
    }

    private boolean canRemovePlayer() {
        return getGame().getPlayers().size() > 4;
    }

    private GameResult handleRound(Round round) {
        getGame().removeRound(round);
        getGame().recalibrateScores();
        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        return getScoreBoard();
    }

    @Override
    public State nextState() {
        if (nextStateDecision == 1) return new CountState(getGame());
        return null;
    }
}