package com.example.betting.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** Settlement command published to the bet-settlements topic. */
public record BetSettlement(
        long betId,
        long userId,
        long eventId,
        String eventName,
        String eventMarketId,
        long eventWinnerId,
        BigDecimal betAmount) {

    @JsonCreator
    public BetSettlement {
    }

    public static BetSettlement from(Bet bet, EventOutcome outcome) {
        return new BetSettlement(
                bet.betId(),
                bet.userId(),
                bet.eventId(),
                outcome.eventName(),
                bet.eventMarketId(),
                bet.eventWinnerId(),
                bet.betAmount());
    }

    @JsonProperty("betId")
    public long betId() {
        return betId;
    }
}
