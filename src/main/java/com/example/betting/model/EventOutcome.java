package com.example.betting.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Event outcome accepted by the HTTP API and published to Kafka. */
public record EventOutcome(

        @NotNull(message = "eventId is required") @Positive Long eventId,

        @NotBlank(message = "eventName is required") String eventName,

        @NotNull(message = "eventWinnerId is required") @Positive Long eventWinnerId) {
}
