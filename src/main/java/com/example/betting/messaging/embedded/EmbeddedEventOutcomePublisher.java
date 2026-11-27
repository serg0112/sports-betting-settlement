package com.example.betting.messaging.embedded;

import com.example.betting.config.BettingProperties;
import com.example.betting.exception.EventPublishException;
import com.example.betting.messaging.EventOutcomePublisher;
import com.example.betting.model.EventOutcome;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "embedded")
public class EmbeddedEventOutcomePublisher implements EventOutcomePublisher {

    private final EmbeddedMessageBroker broker;
    private final ObjectMapper mapper;
    private final String topic;

    public EmbeddedEventOutcomePublisher(
            EmbeddedMessageBroker broker,
            ObjectMapper mapper,
            BettingProperties properties) {
        this.broker = broker;
        this.mapper = mapper;
        this.topic = properties.kafka().eventOutcomesTopic();
    }

    @Override
    public void publish(EventOutcome outcome) {
        try {
            broker.publish(topic, mapper.writeValueAsString(outcome));
        } catch (JsonProcessingException exception) {
            throw new EventPublishException("Unable to serialize event outcome", exception);
        }
    }

    @Override
    public String topic() {
        return topic;
    }
}
