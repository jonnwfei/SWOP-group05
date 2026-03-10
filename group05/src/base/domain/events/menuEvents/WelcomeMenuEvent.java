package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

public record WelcomeMenuEvent() implements GameEvent {
    private String renderWelcomeMenuEvent(){
        return "======== WELCOME TO WHIST ===== \n" +
                "Do you want to:\n" +
                "(1) Play a game?\n" +
                "(2) Count the scores for a game?\n";
    }
    //lower bound 1
    //upper bound 2
}
