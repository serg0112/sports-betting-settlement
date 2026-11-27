package com.example.betting.service;

import com.example.betting.config.BettingTopics;
import com.example.betting.messaging.MessagePublisher;
import com.example.betting.model.EventOutcome;
import com.example.betting.model.PublishedEventOutcome;
import org.springframework.stereotype.Service;

/** Publishes validated event outcomes to Kafka. */
@Service
public class OutcomePublishService {

    private final MessagePublisher publisher;

    public OutcomePublishService(MessagePublisher publisher) {
        this.publisher = publisher;
    }

    public PublishedEventOutcome publish(EventOutcome outcome) {
        publisher.publishEventOutcome(outcome);
        return new PublishedEventOutcome(BettingTopics.EVENT_OUTCOMES, outcome.eventId(), "PUBLISHED");
    }
}
