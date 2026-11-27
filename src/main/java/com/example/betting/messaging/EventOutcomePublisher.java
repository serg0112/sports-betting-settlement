package com.example.betting.messaging;

import com.example.betting.model.EventOutcome;

public interface EventOutcomePublisher {

    void publish(EventOutcome outcome);

    String topic();
}
