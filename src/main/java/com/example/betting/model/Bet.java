package com.example.betting.model;

import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("BETS")
public record Bet(
        @Id long betId,
        long userId,
        long eventId,
        String eventMarketId,
        long eventWinnerId,
        BigDecimal betAmount,
        BetStatus status) {

    public Bet {
        if (betId <= 0 || userId <= 0 || eventId <= 0 || eventWinnerId <= 0) {
            throw new IllegalArgumentException("Bet identifiers must be positive");
        }
        if (eventMarketId == null || eventMarketId.isBlank()) {
            throw new IllegalArgumentException("eventMarketId is required");
        }
        if (betAmount == null || betAmount.signum() <= 0 || betAmount.scale() > 2) {
            throw new IllegalArgumentException("betAmount must be positive with at most two decimal places");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
    }

    public BetStatus statusFor(long actualWinnerId) {
        return eventWinnerId == actualWinnerId ? BetStatus.WON : BetStatus.LOST;
    }

    public Bet withStatus(BetStatus newStatus) {
        return new Bet(betId, userId, eventId, eventMarketId, eventWinnerId, betAmount, newStatus);
    }
}
