package cli.elements;

import base.domain.player.Player;

import javax.swing.*;

public record RejectedProposalEvent(Player proposer) implements GameEvent{
    private void RenderRejectedProposalEvent(RejectedProposalEvent event) {
        System.out.println(proposer().getName() + ": no one accepted your proposal.");
        System.out.println("Choose [0] PASS or [1] SOLO_PROPOSAL:");
    }
}
