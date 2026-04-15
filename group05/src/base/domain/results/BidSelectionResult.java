package base.domain.results;

import base.domain.bid.BidType;


import base.domain.player.Player;

import java.util.List;
import java.util.Objects;

public record BidSelectionResult(
        BidType[] availableBids,
        List<Player> players
) implements GameResult {

    public BidSelectionResult {
        // Null checks
        Objects.requireNonNull(availableBids, "availableBids cannot be null");
        Objects.requireNonNull(players, "players cannot be null");

        // Defensive copies
        availableBids = availableBids.clone();
        players = List.copyOf(players);
    }

    // Optional: extra safety for array getter
    @Override
    public BidType[] availableBids() {
        return availableBids.clone();
    }
}