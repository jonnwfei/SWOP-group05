package cli.elements;

import base.domain.card.Card;
import base.domain.player.Player;

public record EndOfRoundEvent(Player player , Card card) implements GameEvent {
    private String handleEndOfRoundEvent (){
        return player.getName() + " played " + card.toString() + "\n \n============== ROUND OVER ============== \n Calculating final scores...\n";
    }
}
