package com.example.betting.model;

import org.springframework.data.annotation.Id;

public record ProcessedEventOutcome(
        long eventId,
        String eventName,
        long eventWinnerId) {

    @Id
    @Override
    public long eventId() {
        return eventId;
    }
}
