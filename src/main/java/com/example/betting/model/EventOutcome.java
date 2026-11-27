package com.example.betting.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EventOutcome(
        @NotNull(message = "eventId is required") @Positive(message = "must be greater than 0") Long eventId,
        @NotBlank(message = "eventName is required") @Size(max = 255) String eventName,
        @NotNull(message = "eventWinnerId is required") @Positive(message = "must be greater than 0") Long eventWinnerId) {
}
