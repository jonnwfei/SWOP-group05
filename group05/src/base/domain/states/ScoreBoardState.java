package base.domain.states;

import base.domain.WhistGame;
import base.domain.commands.*;
import base.domain.strategy.HighBotStrategy;
import base.domain.strategy.HumanStrategy;
import base.domain.strategy.LowBotStrategy;
import base.domain.player.Player;
import base.domain.results.*;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;

import java.util.List;

import base.domain.commands.*;
import base.domain.results.*;

/**
 * Handles the end-of-round scoreboard display and provides options for where
 * they can go next.
 * 
 * @author John Cai
 * @since 09/03/2026
 */
public class ScoreBoardState extends State {

    private enum Phase {
        SHOW,
        SAVE_DESCRIPTION,
        ADD_PLAYER_TYPE,
        ADD_PLAYER_NAME,
        REMOVE_PLAYER,
        REMOVE_ROUND
    }

    private Phase phase = Phase.SHOW;
    private int choice = 0;

    private final GamePersistenceService persistenceService = new GamePersistenceService();

    public ScoreBoardState(WhistGame game) {
        super(game);
    }

    /**
     * Executes the scoreboard state without user input, typically when first
     * entering the state.
     *
     * @return a GameEvent
     */
    @Override
    public StateStep executeState() {
        // If waiting for save description
        if (phase == Phase.SAVE_DESCRIPTION) {
            return StateStep.stay(new SaveDescriptionResult());
        }
        // No command → just render scoreboard
        return buildScoreBoard();
    }

    /**
     * Processes the scoreboard interaction.
     *
     * @param command The user action
     * @return a GameEvent
     */
    @Override
    public StateStep executeState(GameCommand command) {

        return switch (phase) {

            // =========================================
            // MAIN SCOREBOARD
            // =========================================
            case SHOW -> switch (command) {

                case NumberCommand n -> {
                    int input = n.choice();

                    yield switch (input) {
                        case 1, 2 -> {
                            this.choice = input;
                            yield StateStep.transitionWithoutResult();
                        }
                        case 3 -> {
                            phase = Phase.SAVE_DESCRIPTION;
                            yield StateStep.stay(new SaveDescriptionResult());
                        }
                        case 4 -> {
                            phase = Phase.REMOVE_ROUND;
                            yield StateStep.stay(new DeleteRoundResult(getGame().getRounds()));
                        }
                        case 5 -> {
                            phase = Phase.ADD_PLAYER_TYPE;
                            yield StateStep.stay(new AddPlayerResult());
                        }
                        case 6 -> {
                            phase = Phase.REMOVE_PLAYER;
                            yield StateStep.stay(new PlayerSelectionResult(getGame().getPlayers(), false));
                        }
                        default -> throw new IllegalStateException("Invalid scoreboard input: " + input);
                    };
                }

                default -> buildScoreBoard();
            };

            // =========================================
            // SAVE DESCRIPTION
            // =========================================
            case SAVE_DESCRIPTION -> switch (command) {

                case TextCommand t -> {
                    persistenceService.save(getGame(), SaveMode.GAME, t.text());
                    phase = Phase.SHOW;
                    yield buildScoreBoard();
                }
                default -> StateStep.stay(new SaveDescriptionResult());
            };

            // =========================================
            // ADD PLAYER TYPE
            // =========================================
            case ADD_PLAYER_TYPE -> switch (command) {

                case NumberCommand n -> {
                    switch (n.choice()) {

                        case 1 -> {
                            phase = Phase.ADD_PLAYER_NAME;
                            yield StateStep.stay(new AddHumanPlayerResult()); // UI will return PlayerListCommand
                        }

                        case 2 -> {
                            getGame().addPlayer(new Player(new HighBotStrategy(), "Smart bot")); // TODO: change to SmartBot after TOmmy merge
                            phase = Phase.SHOW;
                            yield buildScoreBoard();
                        }

                        case 3 -> {
                            getGame().addPlayer(new Player(new HighBotStrategy(), "High bot"));
                            phase = Phase.SHOW;
                            yield buildScoreBoard();
                        }

                        case 4 -> {
                            getGame().addPlayer(new Player(new LowBotStrategy(), "Low bot"));
                            phase = Phase.SHOW;
                            yield buildScoreBoard();
                        }

                        default -> throw new IllegalStateException("Invalid player type choice");
                    }
                }

                default -> StateStep.stay(new AddPlayerResult());
            };

            // =========================================
            // ADD HUMAN PLAYER
            // =========================================
            case ADD_PLAYER_NAME -> switch (command) {

                case TextCommand t -> {
                    String name = t.text().trim();
                    if (name.isBlank()) {
                        yield StateStep.stay(new AddHumanPlayerResult()); // retry
                    }

                    Player newPlayer = new Player(new HumanStrategy(), name);
                    getGame().addPlayer(newPlayer);
                    phase = Phase.SHOW;
                    yield buildScoreBoard();
                }

                default -> StateStep.stay(new AddHumanPlayerResult());
            };
            case REMOVE_PLAYER -> switch (command) {
                case PlayerListCommand p -> {
                    if (p.playerIds().isEmpty()) {
                        yield StateStep.stay(new PlayerSelectionResult(getGame().getPlayers(), false)); // retry
                    }

                    Player newPlayer = getGame().getPlayerById(p.playerIds().getFirst());
                    getGame().removePlayer(newPlayer);
                    phase = Phase.SHOW;
                    yield buildScoreBoard();
                }

                default -> StateStep.stay(new PlayerSelectionResult(getGame().getPlayers(), false));
            };
            case REMOVE_ROUND -> switch (command) {
                case RoundCommand r -> {
                    getGame().removeRound(r.round());
                    getGame().recalibrateScores();
                    phase = Phase.SHOW;
                    yield buildScoreBoard();
                }
                default -> StateStep.stay(new DeleteRoundResult(getGame().getRounds()));
            };
        };
    }

    private StateStep buildScoreBoard() {
        List<String> names = getGame().getPlayers().stream().map(Player::getName).toList();
        List<Integer> scores = getGame().getPlayers().stream().map(Player::getScore).toList();
        boolean canRemove = getGame().getPlayers().size() > 4;

        return StateStep.stay(new ScoreBoardResult(names, scores, canRemove));
    }

    @Override
    public State nextState() {
        if (choice == 2)
            return null;

        if (choice == 1) {
            getGame().advanceDealer();
            return new BidState(this.getGame());
        }

        return this;
    }
}