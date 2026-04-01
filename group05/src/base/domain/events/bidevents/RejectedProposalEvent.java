package base.domain.events.bidevents;

import base.domain.events.GameEvent;

/**
 * A Data Transfer Object (DTO) sent when a Proposal bid fails to find a
 * partner.
 * This event triggers a specialized binary-choice menu in the IO.
 *
 * @param proposerName The name of the player who must decide how to resolve
 *                     their rejected proposal.
 */
public record RejectedProposalEvent(String proposerName) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        return input == 1 || input == 2;
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}
