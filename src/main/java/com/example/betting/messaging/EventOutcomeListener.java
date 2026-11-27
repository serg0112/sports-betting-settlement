package com.example.betting.messaging;

import com.example.betting.model.EventOutcome;

/** Kafka consumer boundary for event-outcomes. */
public interface EventOutcomeListener {

    void start(java.util.function.Consumer<EventOutcome> handler);
}
