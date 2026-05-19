package base.domain;

import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.round.Round;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//TODO: hookup to WhistGame after merges
public class WhistGameHistory {
    private final List<Round> rounds;

    public WhistGameHistory() {
        this.rounds = new ArrayList<>();
    }

    public void addRound(Round round) {
        this.rounds.add(round);
    }

    public void removeRound(Round round) {
        this.rounds.remove(round);
    }

    public void clearHistory() {
        this.rounds.clear();
    }

    public List<Round> getRoundHistory() {
        return List.copyOf(this.rounds);
    }

    public Set<Player> getHistoricalPlayers() {
        Set<Player> players = new HashSet<>();
        for (Round round : this.rounds) {
            players.addAll(round.getPlayers());
        }
        return players;
    }

    /**
     * Recalculates all player scores from scratch based on the current round history.
     * Call this after removing a round to ensure the scoreboard is accurate.
     */
    //TODO: also recalibrate for different points (use case 9) prob should happen automatically in Rpund and here nothing changes
    public void recalibrateScores() {
        // 1. Reset all players to 0
        for (Player p : this.getHistoricalPlayers()) {
            p.updateScore(-p.getScore());
        }

        List<PlayerId> currentPlayerIds = this.getHistoricalPlayers().stream().map(Player::getId).toList();

        // 2. Re-apply deltas from all rounds currently in the list
        for (Round round : this.rounds) {
            List<Integer> deltas = round.getScoreDeltas();
            List<Player> roundPlayers = round.getPlayers();

            // Map the deltas back to the players based on their index in the round
            for (int i = 0; i < roundPlayers.size(); i++) {
                Player currentPlayer = roundPlayers.get(i);

                int currentDelta = deltas.get(i);

                currentPlayer.updateScore(currentDelta);
            }
        }
    }

    //TODO: add possible functions to support use case 10 & 11, arguably responsible for making GameSnapshots?
}
