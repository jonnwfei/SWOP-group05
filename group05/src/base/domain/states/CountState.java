package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.commands.*;
import base.domain.events.ErrorEvent;
import base.domain.events.countEvents.*;
import base.domain.events.menuEvents.SaveDescriptionEvent;
import base.domain.results.*;
import base.storage.GamePersistenceService;
import base.domain.snapshots.SaveMode;
import base.domain.player.Player;
import base.domain.events.GameEvent;
import base.domain.bid.*;
import base.domain.card.Suit;
import base.domain.round.Round;

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
        START, SELECT_BID, SELECT_TRUMP, SELECT_PLAYERS, SELECT_WINNERS, CALCULATE, PROMPT_NEXT_STATE, SAVE_DESCRIPTION
    }

    private CountPhase currentPhase = CountPhase.START;
    private int keuze;
    private Bid bid;
    private BidType selectedBidType;
    private Suit trumpSuit;
    private List<Player> participatingPlayers;
    private final GamePersistenceService persistenceService;

    public CountState(WhistGame game) {
        super(game);
        this.persistenceService = new GamePersistenceService();
    }

    /**
     * Routes the user action to the current phase of the scoring wizard.
     *
     * @param command The user input.
     * @return The next event in the scoring sequence.
     */
    @Override
    public GameResult executeState(GameCommand command) {
        if (currentPhase == CountPhase.START) {
            currentPhase = CountPhase.SELECT_BID;
            return new BidSelectionResult(values(), getGame().getPlayers());
        }

        return switch (command) {
            case BidCommand b            -> handleBidType(b.bid());
            case SuitCommand s           -> handleSuit(s.suit());
            case PlayerListCommand p     -> handlePlayerInput(p.players());
            case NumberCommand n         -> handleNumberInput(n.choice());
            case TextCommand t           -> handleSaveDescription(t.text());
            case ContinueCommand ignored -> nextStep();
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
    }


    private GameResult handleNumberInput(int value) {
        if (currentPhase == CountPhase.CALCULATE) {
            // normal bid path, no winners
            return finalizeCalculation(value, null);
        }
        // PROMPT_NEXT_STATE
        if (value == 3) {
            currentPhase = CountPhase.SAVE_DESCRIPTION;
            return new SaveDescriptionResult();
        }
        this.keuze = value;
        return getScoreBoard();
    }


    private GameResult nextStep() {
        currentPhase = CountPhase.SELECT_BID;
        return new BidSelectionResult(values(), getGame().getPlayers());
    }
    /**
     * Initializes the wizard flow.
     *
     * @return GameEvent
     */
    private GameEvent<?> handleStart() {
        currentPhase = CountPhase.SELECT_BID;
        return new WelcomeCountEvent();
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
    private GameResult handlePlayerInput(List<Player> players) {
        if (currentPhase == CountPhase.SELECT_PLAYERS) {
            this.participatingPlayers = players;
            if (bid.getType() == MISERIE || bid.getType()  == OPEN_MISERIE) {
                currentPhase = CountPhase.SELECT_WINNERS;
                return new PlayerSelectionResult(getGame().getPlayers(), true);
            }
            currentPhase = CountPhase.CALCULATE;
            return new TrickInputResult();
        }
        // SELECT_WINNERS
        return finalizeCalculation(0, players);
    }

    /**
     * Performs final score calculation based on tricks won or miserie results.
     *
     * @return GameEvent
     */
    private GameResult finalizeCalculation(int tricks, List<Player> winners) {
        Player primaryBidder = participatingPlayers.getFirst();
        Bid bid = buildBid(primaryBidder);

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

    /**
     * Final prompt to decide whether to count another round or exit.
     *
     * @return GameEvent
     */
    private GameEvent<?> handlePromptNextState(GameAction action) {
        Integer choice = switch (action) {
            case NumberAction(int value) -> value;
            default -> null;
        };
        if (choice == null || choice < 1 || choice > 3) {
            return new ErrorEvent(1, 3);
        }
        if (choice == 3) {
            currentPhase = CountPhase.SAVE_DESCRIPTION;
            return new SaveDescriptionEvent("count");
        }

        this.keuze = choice;
        return new EndOfCountStateEvent();
    }

    private GameResult handleSaveDescription(String text) {
        persistenceService.save(getGame(), SaveMode.COUNT, text);
        currentPhase = CountPhase.PROMPT_NEXT_STATE;
        return getScoreBoard();

    }


    // Bid construction stays in domain — it knows BidType, Player, Suit
    private Bid buildBid(Player bidder) {
        return switch (selectedBidType) {
            case SOLO_PROPOSAL   -> new SoloProposalBid(bidder);
            case PROPOSAL        -> new ProposalBid(bidder);
            case ABONDANCE_9     -> new AbondanceBid(bidder, ABONDANCE_9, trumpSuit);
            case ABONDANCE_10    -> new AbondanceBid(bidder, ABONDANCE_10, trumpSuit);
            case ABONDANCE_11    -> new AbondanceBid(bidder, ABONDANCE_11, trumpSuit);
            case ABONDANCE_12_OT -> new AbondanceBid(bidder, ABONDANCE_12_OT, trumpSuit);
            case MISERIE         -> new MiserieBid(bidder, MISERIE);
            case OPEN_MISERIE    -> new MiserieBid(bidder, OPEN_MISERIE);
            case SOLO            -> new SoloBid(bidder, SOLO, trumpSuit);
            case SOLO_SLIM       -> new SoloBid(bidder, SOLO_SLIM, trumpSuit);
            default -> throw new IllegalStateException("Unknown BidType: " + selectedBidType);
        };
    }
    private List<String> getPlayerNames() {
        return getGame().getPlayers().stream().map(Player::getName).toList();
    }

    /** Returns to a fresh CountState or the Main Menu. */
    @Override
    public State nextState() {
        return (keuze == 1) ? new CountState(getGame()) : new MenuState(getGame());
    }
}