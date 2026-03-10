package base.domain.events.countEvents;

import base.domain.events.GameEvent;

public record WelcomeCountEvent() implements GameEvent {
    private String renderWelcomeCountEvent(){
        return """
                ===== WELCOME TO THE COUNT ====\s
                 WHICH ROUND WAS PLAYED?\s
                Proposal:\s
                (1) Alone    (2) With Partner
                Abondance:
                (3) 9   (4) 10   (5) 11   (6) 12
                Miserie:
                (7) Normal       (8) Open
                Solo:
                (9) Normal       (10) Solo Slim
                """;
    }
    //lower 1
    //higher 10
}
