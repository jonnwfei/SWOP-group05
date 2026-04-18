package base.domain.states;

import base.domain.WhistGame;
import base.domain.commands.*;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.results.*;
import base.domain.strategy.HighBotStrategy;
import base.domain.strategy.HumanStrategy;
import base.domain.strategy.LowBotStrategy;
import base.domain.strategy.SmartBotStrategy;

import java.util.List;

/**
 * Handles the end-of-round scoreboard display and routes user choices.
 * <p>
 * IO-agnostic: emits {@link GameSaveDescriptionResult} to request a save and
 * relies on the adapter to actually persist. When the follow-up
 * {@link TextCommand} arrives the save has already happened.
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

    public ScoreBoardState(WhistGame game) {
        super(game);
    }

    @Override
    public StateStep executeState() {
        if (phase == Phase.SAVE_DESCRIPTION) {
            return StateStep.stay(new GameSaveDescriptionResult());
        }
        return buildScoreBoard();
    }

    @Override
    public StateStep executeState(GameCommand command) {

        return switch (phase) {

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
                            yield StateStep.stay(new GameSaveDescriptionResult());
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
                            yield StateStep.stay(new PlayerSelectionResult(getGame().getPlayers()));
                        }
                        default -> throw new IllegalStateException("Invalid scoreboard input: " + input);
                    };
                }
                default -> buildScoreBoard();
            };

            case SAVE_DESCRIPTION -> switch (command) {
                // Adapter has already persisted by the time we see this TextCommand.
                case TextCommand ignored -> {
                    phase = Phase.SHOW;
                    yield buildScoreBoard();
                }
                default -> StateStep.stay(new GameSaveDescriptionResult());
            };

            case ADD_PLAYER_TYPE -> switch (command) {
                case NumberCommand n -> {
                    switch (n.choice()) {
                        case 1 -> {
                            phase = Phase.ADD_PLAYER_NAME;
                            yield StateStep.stay(new AddHumanPlayerResult());
                        }
                        case 2 -> {
                            PlayerId playerId = new PlayerId();
                            getGame().addPlayer(new Player(new SmartBotStrategy(playerId), "Smart bot", playerId));
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

            case ADD_PLAYER_NAME -> switch (command) {
                case TextCommand t -> {
                    String name = t.text().trim();
                    if (name.isBlank()) {
                        yield StateStep.stay(new AddHumanPlayerResult());
                    }
                    getGame().addPlayer(new Player(new HumanStrategy(), name));
                    phase = Phase.SHOW;
                    yield buildScoreBoard();
                }
                default -> StateStep.stay(new AddHumanPlayerResult());
            };

            case REMOVE_PLAYER -> switch (command) {
                case PlayerListCommand p -> {
                    if (p.playerIds().isEmpty()) {
                        yield StateStep.stay(new PlayerSelectionResult(getGame().getPlayers()));
                    }
                    Player newPlayer = getGame().getPlayerById(p.playerIds().getFirst());
                    getGame().removePlayer(newPlayer);
                    phase = Phase.SHOW;
                    yield buildScoreBoard();
                }
                default -> StateStep.stay(new PlayerSelectionResult(getGame().getPlayers()));
            };

            case REMOVE_ROUND -> switch (command) {
                case NumberCommand n -> {
                    if (n.choice() == 0) {
                        phase = Phase.SHOW;
                        yield buildScoreBoard();
                    }
                    yield StateStep.stay(new DeleteRoundResult(getGame().getRounds()));
                }
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
        if (choice == 2) return null;
        if (choice == 1) {
            getGame().advanceDealer();
            return new BidState(this.getGame());
        }
        return this;
    }
}