package com.example.betting.model;

import java.math.BigDecimal;

public record BetSettlement(
        long betId,
        long userId,
        long eventId,
        String eventName,
        String eventMarketId,
        long predictedWinnerId,
        long actualWinnerId,
        BigDecimal betAmount) {

    public static BetSettlement from(Bet bet, EventOutcome outcome) {
        return new BetSettlement(
                bet.betId(),
                bet.userId(),
                bet.eventId(),
                outcome.eventName(),
                bet.eventMarketId(),
                bet.eventWinnerId(),
                outcome.eventWinnerId(),
                bet.betAmount());
    }
}
