package cli.flows;

import base.GameController;
import base.domain.round.Round;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import cli.util.TerminalInputHelper;
import cli.TerminalManager;

import static cli.events.CountEvents.*;
import static cli.events.MenuEvents.*;

import java.util.List;

public class GameEditFlow {

    private final TerminalInputHelper input;
    private final GameController controller;
    private final GamePersistenceService persistenceService;
    private SaveMode mode;

    public GameEditFlow(TerminalManager terminalManager, GameController controller,
                        GamePersistenceService persistenceService, SaveMode mode) {
        if (terminalManager == null)    throw new IllegalArgumentException("terminalManager cannot be null");
        if (controller == null)         throw new IllegalArgumentException("controller cannot be null");
        if (persistenceService == null) throw new IllegalArgumentException("persistenceService cannot be null");
        if (mode == null)               throw new IllegalArgumentException("mode cannot be null");
        this.input = new TerminalInputHelper(terminalManager);
        this.controller = controller;
        this.persistenceService = persistenceService;
        this.mode = mode;
    }

    public void saveGame() {
        String description = input.askString(new SaveDescriptionIOEvent());
        try {
            persistenceService.save(controller.getGame(), mode, description);
        } catch (RuntimeException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    public void addPlayer() {
        if (mode == SaveMode.COUNT) {
            controller.addHumanPlayer(input.askString(new AddHumanPlayerIOEvent()));
            return;
        }
        int type = input.askInt(new AddPlayerIOEvent(), 1, 4);
        switch (type) {
            case 1 -> controller.addHumanPlayer(input.askString(new AddHumanPlayerIOEvent()));
            case 2 -> controller.addSmartBot("Smart bot");
            case 3 -> controller.addHighBot("High bot");
            case 4 -> controller.addLowBot("Low bot");
        }
    }

    public boolean removePlayer() {
        if (!controller.canRemovePlayer()) return false;
        List<Integer> indices = input.askIntList(new PlayerSelectionIOEvent(controller.getAllPlayers(), false, null));
        if (indices.isEmpty()) return false;
        int idx = indices.getFirst();
        if (idx < 1 || idx > controller.getPlayerCount()) return false;
        controller.removePlayerAtIndex(idx - 1);
        return true;
    }

    public boolean removeRound() {
        List<Round> rounds = controller.getRounds();
        if (rounds.isEmpty()) return false;
        int choice = input.askInt(new DeleteRoundIOEvent(rounds), 0, rounds.size());
        if (choice == 0) return false;
        controller.removeRound(rounds.get(choice - 1));
        controller.recalibrateScores();
        return true;
    }

    public void setMode(SaveMode mode) {
        if (mode == null) throw new IllegalArgumentException("mode cannot be null");
        this.mode = mode;
    }
}