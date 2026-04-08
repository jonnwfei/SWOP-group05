package base.domain.states;

import base.domain.WhistGame;
import base.domain.results.*;
import base.domain.commands.*;
import base.storage.GamePersistenceService;
import base.domain.snapshots.SaveMode;
import base.domain.player.Player;
import base.domain.bid.*;
import base.domain.card.Suit;
import base.domain.round.Round;

import java.util.List;

public class CountState extends State {

    private enum CountPhase {
        START, SELECT_BID, SELECT_TRUMP, SELECT_PLAYERS, SELECT_WINNERS, CALCULATE, PROMPT_NEXT_STATE, SAVE_DESCRIPTION
    }

    private CountPhase currentPhase = CountPhase.START;
    private BidType selectedBidType;
    private Suit trumpSuit;
    private List<Player> participatingPlayers;
    private int menuChoice = 0;
    private final GamePersistenceService persistenceService;

    public CountState(WhistGame game) {
        super(game);
        this.persistenceService = new GamePersistenceService();
    }

    @Override
    public GameResult executeState(GameCommand command) {
        if (currentPhase == CountPhase.START) {
            currentPhase = CountPhase.SELECT_BID;
            return new BidSelectionResult(BidType.values());
        }

        return switch (command) {
            case BidCommand b    -> handleBidType(b.bid());
            case SuitCommand s   -> handleSuit(s.suit());
            case PlayerListCommand p -> handlePlayerInput(p.players());
            case NumberCommand n -> handleNumberInput(n.value());
            case TextCommand t   -> handleSaveDescription(t.text());
            case ContinueCommand c -> nextStep();
            default -> new ErrorResult("Ongeldige actie voor deze fase.");
        };
    }

    private GameResult handleBidType(BidType type) {
        this.selectedBidType = type;
        if (type == BidType.MISERIE || type == BidType.OPEN_MISERIE) {
            currentPhase = CountPhase.SELECT_PLAYERS;
            return new PlayerSelectionResult(getPlayerNames(), "Wie speelt er mee?", true);
        }
        currentPhase = CountPhase.SELECT_TRUMP;
        return new SuitSelectionResult();
    }

    private GameResult handleSuit(Suit suit) {
        this.trumpSuit = suit;
        currentPhase = CountPhase.SELECT_PLAYERS;
        return new PlayerSelectionResult(getPlayerNames(), "Wie is de hoofdbieder?", false);
    }

    private GameResult handlePlayerInput(List<Player> players) {
        if (currentPhase == CountPhase.SELECT_PLAYERS) {
            this.participatingPlayers = players;
            if (selectedBidType == BidType.MISERIE || selectedBidType == BidType.OPEN_MISERIE) {
                currentPhase = CountPhase.SELECT_WINNERS;
                return new PlayerSelectionResult(getPlayerNames(), "Wie heeft er gewonnen?", true);
            }
            currentPhase = CountPhase.CALCULATE;
            return new TrickInputResult();
        }

        if (currentPhase == CountPhase.SELECT_WINNERS) {
            return finalizeCalculation(0, players);
        }

        return new ErrorResult("Speler-input niet verwacht.");
    }

    private GameResult handleNumberInput(int value) {
        if (currentPhase == CountPhase.CALCULATE) {
            return finalizeCalculation(value, null);
        }
        if (currentPhase == CountPhase.PROMPT_NEXT_STATE) {
            if (value == 3) {
                currentPhase = CountPhase.SAVE_DESCRIPTION;
                return new SaveDescriptionResult();
            }
            this.menuChoice = value;
            return getScoreBoard();
        }
        return new ErrorResult("Getal niet verwacht.");
    }

    private GameResult finalizeCalculation(int tricks, List<Player> winners) {
        Player primaryBidder = participatingPlayers.get(0);
        Bid bid = createBidObject(primaryBidder);

        Round round = new Round(getGame().getPlayers(), primaryBidder, 1);
        round.setHighestBid(bid);
        getGame().addRound(round);

        round.calculateScoresForCount(tricks, participatingPlayers, winners);

        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        return getScoreBoard();
    }

    private GameResult handleSaveDescription(String text) {
        if (currentPhase == CountPhase.SAVE_DESCRIPTION) {
            persistenceService.save(getGame(), SaveMode.COUNT, text);
            currentPhase = CountPhase.PROMPT_NEXT_STATE;
            return getScoreBoard();
        }
        return new ErrorResult("Tekst niet verwacht.");
    }

    private GameResult nextStep() {
        return switch (currentPhase) {
            case START -> executeState(null);
            default -> new ErrorResult("Druk op een toets om verder te gaan.");
        };
    }

    private Bid createBidObject(Player bidder) {
        return switch (selectedBidType) {
            case SOLO_PROPOSAL -> new SoloProposalBid(bidder);
            case PROPOSAL      -> new ProposalBid(bidder);
            case MISERIE       -> new MiserieBid(bidder, BidType.MISERIE);
            case OPEN_MISERIE  -> new MiserieBid(bidder, BidType.OPEN_MISERIE);
            case ABONDANCE_9   -> new AbondanceBid(bidder, BidType.ABONDANCE_9, trumpSuit);
            case ABONDANCE_10  -> new AbondanceBid(bidder, BidType.ABONDANCE_10, trumpSuit);
            case ABONDANCE_11  -> new AbondanceBid(bidder, BidType.ABONDANCE_11, trumpSuit);
            case ABONDANCE_12_OT -> new AbondanceBid(bidder, BidType.ABONDANCE_12_OT, trumpSuit);
            case SOLO          -> new SoloBid(bidder, BidType.SOLO, trumpSuit);
            case SOLO_SLIM     -> new SoloBid(bidder, BidType.SOLO_SLIM, trumpSuit);
            case PASS          -> new PassBid(bidder);
            default -> throw new IllegalStateException("Onbekend BidType: " + selectedBidType);
        };
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
        return (menuChoice == 1) ? new CountState(getGame()) : new MenuState(getGame());
    }
}