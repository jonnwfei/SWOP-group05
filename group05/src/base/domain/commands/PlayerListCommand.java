package base.domain.commands;

import base.domain.bid.BidType;
import base.domain.player.Player;

import java.util.List;

public record PlayerListCommand(List<Player> players) implements GameCommand { }
