package cli.events;

import base.domain.results.BidResults.*;
import base.domain.results.CountResults.*;
import base.domain.results.PlayResults.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BidEventsTest {

    @Test
    void biddingCompletedIOEvent_doesNotNeedInput() {
        // Act
        BidEvents.BiddingCompletedIOEvent event = new BidEvents.BiddingCompletedIOEvent();

        // Assert
        assertFalse(event.needsInput(), "BiddingCompletedIOEvent should NOT need input");
        assertInstanceOf(BidEvents.class, event, "Event should implement BidEvents");
    }

    @Test
    void bidTurnIOEvent_storesDataAndNeedsInput() {
        // Arrange
        BidTurnResult mockResult = mock(BidTurnResult.class);

        // Act
        BidEvents.BidTurnIOEvent event = new BidEvents.BidTurnIOEvent(mockResult);

        // Assert
        assertEquals(mockResult, event.data(), "Should store the exact BidTurnResult object");
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(BidEvents.class, event, "Event should implement BidEvents");
    }

    @Test
    void proposalRejectedIOEvent_storesDataAndNeedsInput() {
        // Arrange
        ProposalRejected mockRejected = mock(ProposalRejected.class);

        // Act
        BidEvents.ProposalRejectedIOEvent event = new BidEvents.ProposalRejectedIOEvent(mockRejected);

        // Assert
        assertEquals(mockRejected, event.data(), "Should store the exact ProposalRejected object");
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(BidEvents.class, event, "Event should implement BidEvents");
    }

    @Test
    void suitSelectionIOEvent_needsInput() {
        // Act
        BidEvents.SuitSelectionIOEvent event = new BidEvents.SuitSelectionIOEvent();

        // Assert
        assertTrue(event.needsInput(), "needsInput() must return true");
        assertInstanceOf(BidEvents.class, event, "Event should implement BidEvents");
    }
}