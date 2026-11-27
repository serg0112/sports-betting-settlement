package com.example.betting.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BetSettlement(
        @Positive(message = "betId must be greater than 0") long betId,
        @Positive(message = "userId must be greater than 0") long userId,
        @Positive(message = "eventId must be greater than 0") long eventId,
        @NotBlank(message = "eventName is required") @Size(max = 255) String eventName,
        @NotBlank(message = "eventMarketId is required") @Size(max = 64) String eventMarketId,
        @Positive(message = "predictedWinnerId must be greater than 0") long predictedWinnerId,
        @Positive(message = "actualWinnerId must be greater than 0") long actualWinnerId,
        @NotNull(message = "betAmount is required")
        @DecimalMin(value = "0.01", message = "betAmount must be positive")
        @Digits(integer = 17, fraction = 2, message = "betAmount must have at most two decimal places")
        BigDecimal betAmount) {

    public BetSettlement {
        if (eventName == null || eventName.isBlank()) {
            throw new IllegalArgumentException("eventName is required");
        }
        if (eventMarketId == null || eventMarketId.isBlank()) {
            throw new IllegalArgumentException("eventMarketId is required");
        }
        if (betAmount == null || betAmount.signum() <= 0 || betAmount.scale() > 2) {
            throw new IllegalArgumentException("betAmount must be positive with at most two decimal places");
        }
    }

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
