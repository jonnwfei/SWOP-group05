package base.domain.results;

import base.domain.bid.BidType;


import base.domain.player.Player;

import java.util.List;

public record BidSelectionResult(
        BidType[] availableBids,
        List<Player> players
) implements GameResult {

    public BidSelectionResult {
        if (availableBids == null || availableBids.length == 0) {
            throw new IllegalArgumentException("availableBids cannot be null or empty");
        }
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("players cannot be null or empty");
        }

        availableBids = availableBids.clone();
        players = List.copyOf(players);
    }

    @Override
    public BidType[] availableBids() {
        return availableBids.clone();
    }
}