package com.example.betting.messaging;

import com.example.betting.model.BetSettlement;
import com.example.betting.model.EventOutcome;

/** Routes each message type to its configured transport. */
public interface MessagePublisher {

    void publishEventOutcome(EventOutcome outcome);

    void publishBetSettlement(BetSettlement settlement);

    record Composite(
            TopicPublisher<EventOutcome> eventOutcomePublisher,
            TopicPublisher<BetSettlement> betSettlementPublisher) implements MessagePublisher {

        @Override
        public void publishEventOutcome(EventOutcome outcome) {
            eventOutcomePublisher.publish(outcome);
        }

        @Override
        public void publishBetSettlement(BetSettlement settlement) {
            betSettlementPublisher.publish(settlement);
        }
    }
}
