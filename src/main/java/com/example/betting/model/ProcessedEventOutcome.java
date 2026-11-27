package com.example.betting.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("PROCESSED_EVENT_OUTCOMES")
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
