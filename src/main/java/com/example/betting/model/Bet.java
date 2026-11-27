package com.example.betting.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Bet stored in memory until its event outcome is received. */
public record Bet(
        @NotNull @Positive Long betId,
        @NotNull @Positive Long userId,
        @NotNull @Positive Long eventId,
        @NotBlank String eventMarketId,
        @NotNull @Positive Long eventWinnerId,
        @NotNull @Positive BigDecimal betAmount) {
}
