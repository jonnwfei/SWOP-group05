package cli.elements;

import base.domain.card.Card;
import base.domain.player.Player;

public record EndOfTrickEvent(Player player, Card card) implements GameEvent{
    private String handleEndOfTrickEvent(){
       return player.getName() + " played " + card.toString() + "\n============== NEXT TRICK ==============\n";
    }
}
