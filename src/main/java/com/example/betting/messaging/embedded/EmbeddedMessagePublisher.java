package com.example.betting.messaging.embedded;

import com.example.betting.config.BettingTopics;
import com.example.betting.messaging.MessagePublisher;
import com.example.betting.messaging.TopicPublisher;
import com.example.betting.model.BetSettlement;
import com.example.betting.model.EventOutcome;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Publishes typed messages to the in-process broker as JSON strings. */
@Component
@Primary
public class EmbeddedMessagePublisher implements MessagePublisher {

    private final TopicPublisher<EventOutcome> outcomes;
    private final TopicPublisher<BetSettlement> settlements;

    public EmbeddedMessagePublisher(EmbeddedMessageBroker broker, ObjectMapper mapper) {
        this.outcomes = new JsonTopicPublisher<>(broker, mapper, BettingTopics.EVENT_OUTCOMES);
        this.settlements = new JsonTopicPublisher<>(broker, mapper, BettingTopics.BET_SETTLEMENTS);
    }

    @Override
    public void publishEventOutcome(EventOutcome outcome) {
        outcomes.publish(outcome);
    }

    @Override
    public void publishBetSettlement(BetSettlement settlement) {
        settlements.publish(settlement);
    }

    private static final class JsonTopicPublisher<T> implements TopicPublisher<T> {
        private final EmbeddedMessageBroker broker;
        private final ObjectMapper mapper;
        private final String topic;

        private JsonTopicPublisher(EmbeddedMessageBroker broker, ObjectMapper mapper, String topic) {
            this.broker = broker;
            this.mapper = mapper;
            this.topic = topic;
        }

        @Override
        public void publish(T message) {
            try {
                broker.publish(topic, mapper.writeValueAsString(message));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Unable to serialize message for topic " + topic, exception);
            }
        }
    }
}
