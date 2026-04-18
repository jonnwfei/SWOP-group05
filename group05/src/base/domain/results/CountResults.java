package base.domain.results;

import base.domain.bid.BidType;
import base.domain.player.Player;
import base.domain.round.Round;
import cli.events.CountEvents;

import java.util.List;

public sealed interface CountResults extends GameResult {

    record AddPlayerResult() implements CountResults {}

    record AddHumanPlayerResult() implements CountResults {}

    record BidSelectionResult(BidType[] availableBids, List<Player> players) implements CountResults {
        public BidSelectionResult {
            if (availableBids == null || availableBids.length == 0)
                throw new IllegalArgumentException("availableBids cannot be null or empty.");
            if (players == null || players.isEmpty())
                throw new IllegalArgumentException("players cannot be null or empty.");
        }
    }

    record SuitSelectionResult() implements CountResults {}

     record PlayerSelectionResult(
            List<Player> players,
            boolean multiSelect,
            BidType type
    ) implements CountResults {
        public PlayerSelectionResult(List<Player> players) {
            this(players, false, null);
        }


        public PlayerSelectionResult {
            if (players == null || players.isEmpty()) {
                throw new IllegalArgumentException("players cannot be null or empty");
            }
            if (type == null){
                throw new IllegalArgumentException("bidtype cannot be null");
            }
            players = List.copyOf(players);
        }
    }

    record AmountOfTrickWonResult() implements CountResults {}

    record ScoreBoardResult(
            List<String> names,
            List<Integer> scores,
            boolean canRemovePlayer
    ) implements CountResults {
        public ScoreBoardResult {
            if (names == null || names.isEmpty())
                throw new IllegalArgumentException("names cannot be null or empty.");
            if (scores == null || scores.isEmpty())
                throw new IllegalArgumentException("scores cannot be null or empty.");
            if (names.size() != scores.size())
                throw new IllegalArgumentException("names and scores must have the same size.");
            names  = List.copyOf(names);
            scores = List.copyOf(scores);
        }
    }


    record SaveDescriptionResult() implements CountResults {}
    record DeleteRoundResult (List<Round> rounds) implements CountResults{}
}