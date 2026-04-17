package base.domain.states;

import base.domain.WhistGame;

import base.domain.commands.*;

import base.domain.player.PlayerId;
import base.domain.results.*;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import base.domain.player.Player;
import base.domain.strategy.HumanStrategy;

import base.domain.bid.*;
import base.domain.card.Suit;
import base.domain.round.Round;

import java.util.ArrayList;
import java.util.List;

import static base.domain.bid.BidType.*;

/**
 * State responsible for manual score calculation. (use case 1)
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
    private List<PlayerId> participatingPlayerIds; // Upgraded to PlayerId
    private final GamePersistenceService persistenceService;

    public CountState(WhistGame game) {
        super(game);
        this.persistenceService = new GamePersistenceService();
    }

    /**
     * Executes the current phase of the scoring process without user input (e.g.
     * initial entry into CountState).
     *
     * @return The next event in the scoring sequence.
     */
    @Override
    public StateStep executeState() {
        if (currentPhase == CountPhase.START) {
            currentPhase = CountPhase.SELECT_BID;
            return StateStep.stay(new BidSelectionResult(values(), getGame().getPlayers()));
        }

        return StateStep.stay(nextStep());
    }

    /**
     * Routes the user action to the current phase of the scoring wizard.
     *
     * @param command The user input.
     * @return The next event in the scoring sequence.
     */
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
                } else {
                    yield StateStep.stay(handlePlayerInput(p.playerIds()));
                }

            }
            case NumberCommand n -> handleNumberInput(n.choice());
            case TextCommand t -> {
                if (currentPhase == CountPhase.SAVE_DESCRIPTION) {
                    yield StateStep.stay(handleSaveDescription(t.text()));
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

    private GameResult handleRound(Round round) {
        // Remove the round from the game's internal list
        getGame().removeRound(round);

        // Recalculate everyone's score based on the remaining rounds
        getGame().recalibrateScores();

        // Return to the scoreboard menu
        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        return getScoreBoard();
    }

    private StateStep handleNumberInput(int value) {
        if (currentPhase == CountPhase.CALCULATE) {
            return StateStep.stay(finalizeCalculation(value, null));
        }

        // PROMPT_NEXT_STATE
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
                yield StateStep.stay(new SaveDescriptionResult());
            }
            case 4 -> {// remove rounds
                currentPhase = CountPhase.REMOVE_ROUND;
                yield StateStep.stay(new DeleteRoundResult(getGame().getRounds()));
            }
            case 5 -> {
                currentPhase = CountPhase.ADD_PLAYER;
                yield StateStep.stay(new AddHumanPlayerResult());
            }
            case 6 -> {
                currentPhase = CountPhase.REMOVE_PLAYER_SELECT;
                yield StateStep.stay(new PlayerSelectionResult(getGame().getPlayers(), false));
            }
            default -> throw new IllegalStateException("Unexpected number input: " + value);
        };
    }

    private GameResult nextStep() {
        currentPhase = CountPhase.SELECT_BID;
        return new BidSelectionResult(values(), getGame().getPlayers());
    }

    /**
     * Processes the selected bid type
     *
     * @return GameEvent
     */
    private GameResult handleBidType(BidType type) {
        this.selectedBidType = type;

        if (type == MISERIE || type == OPEN_MISERIE) {
            currentPhase = CountPhase.SELECT_PLAYERS;
            return new PlayerSelectionResult(getGame().getPlayers(), true);
        }
        currentPhase = CountPhase.SELECT_TRUMP;
        return new SuitSelectionResult();
    }

    /**
     * Processes the trump suit selection.
     *
     * @return GameEvent
     */
    private GameResult handleSuit(Suit suit) {
        this.trumpSuit = suit;
        currentPhase = CountPhase.SELECT_PLAYERS;
        return new PlayerSelectionResult(getGame().getPlayers(), false);
    }

    /**
     * Identifies which players were involved in the bid.
     *
     * @return GameEvent
     */
    private GameResult handlePlayerInput(List<PlayerId> players) {
        if (currentPhase == CountPhase.SELECT_PLAYERS) {
            // If empty, stay in the same phase and return the selection result again
            if (players == null || players.isEmpty()) {
                // We stay in SELECT_PLAYERS phase
                boolean multiSelect = (selectedBidType == MISERIE || selectedBidType == OPEN_MISERIE);
                return new PlayerSelectionResult(getGame().getPlayers(), multiSelect);
            }

            this.participatingPlayerIds = players;

            // safe to call getFirst() now
            this.bid = selectedBidType.instantiate(participatingPlayerIds.getFirst(), trumpSuit);

            if (selectedBidType == MISERIE || selectedBidType == OPEN_MISERIE) {
                currentPhase = CountPhase.SELECT_WINNERS;
                return new PlayerSelectionResult(getGame().getPlayers(), true);
            }

            currentPhase = CountPhase.CALCULATE;
            return new AmountOfTrickWonResult();
        }

        // SELECT_WINNERS — players here are the winners, participatingPlayers already set
        // An empty list here is fine (means everyone lost their miserie)
        return finalizeCalculation(0, players);
    }

    /**
     * Performs final score calculation based on tricks won or miserie results.
     *
     * @return GameEvent
     */
    private GameResult finalizeCalculation(int tricks, List<PlayerId> winnersId) {
        Player primaryBidder = getGame().getPlayerById(participatingPlayerIds.getFirst());
        Round round = new Round(getGame().getPlayers(), primaryBidder, 1);
        round.setHighestBid(bid);
        getGame().addRound(round);

        List<Player> participatingPlayers = participatingPlayerIds
                .stream()
                .map(playerId -> getGame().getPlayerById(playerId))
                .toList();

        List<Player> winners = new ArrayList<>();
        if (winnersId != null) {
            winners = winnersId.stream().map(playerId -> getGame().getPlayerById(playerId)).toList();
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

    private GameResult handleSaveDescription(String text) {
        try {
            persistenceService.save(getGame(), SaveMode.COUNT, text); // TODO: this needs to be checked and fixed,
                                                                      // shouldnt be in domain layer
        } catch (Exception e) {
            throw new IllegalStateException("Error in CountState handleSaveDescription", e);
        }
        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        return getScoreBoard();
    }

    // Called when TextCommand arrives during ADD_PLAYER
    private GameResult handleAddPlayer(String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isBlank()) {
            return new AddHumanPlayerResult(); // re-prompt
        }

        Player newPlayer = new Player(new HumanStrategy(), cleanName);
        getGame().addPlayer(newPlayer);
        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        return getScoreBoard();
    }

    // Called when PlayerListCommand arrives during REMOVE_PLAYER_SELECT
    private GameResult handleRemovePlayer(List<PlayerId> playerIds) {
        if (!canRemovePlayer()) {
            currentPhase = CountPhase.PROMPT_NEXT_STATE;
            return getScoreBoard();
        }
        if (playerIds.isEmpty()) {
            return new PlayerSelectionResult(getGame().getPlayers(), false); // re-prompt
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

    /** Returns to a fresh CountState or the Main Menu. */
    @Override
    public State nextState() {
        if (nextStateDecision == 1) {
            return new CountState(getGame());
        } else {
            return null;
        }
    }
}