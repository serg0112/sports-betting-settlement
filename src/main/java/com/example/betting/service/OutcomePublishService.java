package com.example.betting.service;

import com.example.betting.messaging.EventOutcomePublisher;
import com.example.betting.model.EventOutcome;
import com.example.betting.model.PublishedEventOutcome;
import org.springframework.stereotype.Service;

@Service
public class OutcomePublishService {

    private final EventOutcomePublisher publisher;

    public OutcomePublishService(EventOutcomePublisher publisher) {
        this.publisher = publisher;
    }

    public PublishedEventOutcome publish(EventOutcome outcome) {
        publisher.publish(outcome);
        return new PublishedEventOutcome(publisher.topic(), outcome.eventId(), "PUBLISHED");
    }
}
