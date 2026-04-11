package base.domain.results;

import base.domain.player.Player;

import java.util.List;

public record PlayerSelectionResult(List<Player> players, boolean multiSelect) implements GameResult {}

