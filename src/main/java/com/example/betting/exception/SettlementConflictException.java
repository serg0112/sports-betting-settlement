package com.example.betting.exception;

import com.example.betting.model.BetStatus;

public class SettlementConflictException extends RuntimeException {

    public SettlementConflictException(long betId, BetStatus currentStatus, BetStatus requestedStatus) {
        super("Conflicting settlement for betId=" + betId
                + ": current status " + currentStatus + ", requested status " + requestedStatus);
    }

    public SettlementConflictException(long betId, long persistedEventId, long settlementEventId) {
        super("Conflicting settlement for betId=" + betId
                + ": eventId=" + settlementEventId + " does not match persisted eventId=" + persistedEventId);
    }
}
