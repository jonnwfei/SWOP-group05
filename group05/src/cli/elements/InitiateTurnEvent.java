package cli.elements;

import base.domain.player.Player;

public record InitiateTurnEvent(Player player) implements GameEvent {
    private   String renderInitiateTurnEvent (){
        return "\n============== Pass the terminal to " + player.getName() + " ==============\n"
                + "Press ANY BUTTON to reveal your hand...";
    }
}
