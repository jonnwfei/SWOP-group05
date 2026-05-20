package base.domain.round;

import base.domain.player.PlayerId;
import base.domain.trick.Trick;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TrickLedger {
    private final List<Trick> tricks = new ArrayList<>();
    //TODO: use WhistRules
    public static final int MAX_TRICKS = 13;

    public void recordTrick(Trick trick) {
        if (trick == null) {
            throw new IllegalArgumentException("Trick must not be null.");
        }
        if (trick.getTurns() == null || trick.getTurns().size() != Trick.MAX_TURNS) {
            throw new IllegalArgumentException("Trick is not completed yet.");
        }
        if (trick.getWinningPlayerId() == null) {
            throw new IllegalArgumentException("Trick must have a winning player before being recorded.");
        }
        if (this.isFull()) {
            throw new IllegalStateException("Cannot add trick: The round is already finished.");
        }
        tricks.add(trick);
    }

    public List<Trick> getTricks() {
        return List.copyOf(tricks);
    }

    public Trick getLastTrick() {
        return tricks.isEmpty() ? null : tricks.getLast();
    }

    public boolean isFull() {
        return tricks.size() == MAX_TRICKS;
    }

    public int getTricksWonByTeam(List<PlayerId> team) {
        Objects.requireNonNull(team, "Team cannot be null.");

        return (int) tricks.stream()
                .filter(t -> team.contains(t.getWinningPlayerId()))
                .count();
    }

    public boolean hasPlayerWonAnyTrick(PlayerId playerId) {
        Objects.requireNonNull(playerId, "PlayerId cannot be null.");

        return tricks.stream().anyMatch(t -> t.getWinningPlayerId().equals(playerId));
    }
}