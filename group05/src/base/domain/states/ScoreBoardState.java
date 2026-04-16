package base.domain.states;

import base.domain.WhistGame;
import base.domain.commands.*;
import base.domain.player.HighBotStrategy;
import base.domain.player.LowBotStrategy;
import base.domain.player.Player;
import base.domain.results.*;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;

import java.util.List;

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

    @Override
    public GameResult executeState(GameCommand command) {

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
                            yield new ScoreBoardCompleteResult();
                        }
                        case 3 -> {
                            phase = Phase.SAVE_DESCRIPTION;
                            yield new SaveDescriptionResult();
                        }
                        case 4 -> {
                            phase = Phase.REMOVE_ROUND;
                            yield new DeleteRoundResult(getGame().getRounds());
                        }
                        case 5 -> {
                            phase = Phase.ADD_PLAYER_TYPE;
                            yield new AddPlayerResult();
                        }
                        case 6 ->{
                            phase = Phase.REMOVE_PLAYER;
                            yield new PlayerSelectionResult(getGame().getPlayers(), false);
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

                default -> new SaveDescriptionResult();
            };

            // =========================================
            // ADD PLAYER TYPE
            // =========================================
            case ADD_PLAYER_TYPE -> switch (command) {

                case NumberCommand n -> {
                    switch (n.choice()) {

                        case 1 -> {
                            phase = Phase.ADD_PLAYER_NAME;
                            yield new AddHumanPlayerResult(); // UI will return PlayerListCommand
                        }

                        case 2 -> {
                            getGame().addPlayer(new Player(new HighBotStrategy(), "Smart bot"));
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

                default -> new AddPlayerResult();
            };

            // =========================================
            // ADD HUMAN PLAYER (via PlayerListCommand!)
            // =========================================
            case ADD_PLAYER_NAME -> switch (command) {

                case PlayerListCommand p -> {
                    if (p.players().isEmpty()) {
                        yield new AddHumanPlayerResult(); // retry
                    }

                    getGame().addPlayer(p.players().getFirst());
                    phase = Phase.SHOW;
                    yield buildScoreBoard();
                }

                default -> new AddHumanPlayerResult();
            };
            case REMOVE_PLAYER ->  switch (command){
                case PlayerListCommand p -> {
                    if (p.players().isEmpty()) {
                        yield new PlayerSelectionResult(getGame().getPlayers(), false); // retry
                    }

                    getGame().removePlayer(p.players().getFirst());
                    phase = Phase.SHOW;
                    yield buildScoreBoard();
                }

                default -> new PlayerSelectionResult(getGame().getPlayers(), false);
                };
            case REMOVE_ROUND -> switch (command){
                case RoundCommand r ->{
                    getGame().removeRound(r.round());
                    getGame().recalibrateScores();
                    phase = Phase.SHOW;
                    yield buildScoreBoard();
                }
                default -> new DeleteRoundResult(getGame().getRounds());
            };
        };
    }

    private GameResult buildScoreBoard() {
        List<String> names = getGame().getPlayers().stream().map(Player::getName).toList();
        List<Integer> scores = getGame().getPlayers().stream().map(Player::getScore).toList();
        boolean canRemove = getGame().getPlayers().size() > 4;

        return new ScoreBoardResult(names, scores, canRemove);
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