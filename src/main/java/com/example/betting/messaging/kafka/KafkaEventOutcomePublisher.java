package com.example.betting.messaging.kafka;

import com.example.betting.config.BettingProperties;
import com.example.betting.messaging.TopicPublisher;
import com.example.betting.model.EventOutcome;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Kafka producer for the event-outcomes named topic. */
@Component
public class KafkaEventOutcomePublisher implements TopicPublisher<EventOutcome> {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;
    private final String topic;

    public KafkaEventOutcomePublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper mapper,
            BettingProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
        this.topic = properties.kafka().eventOutcomesTopic();
    }

    @Override
    public void publish(EventOutcome outcome) {
        try {
            String payload = mapper.writeValueAsString(outcome);
            kafkaTemplate.send(new ProducerRecord<>(topic, String.valueOf(outcome.eventId()), payload));
            kafkaTemplate.flush();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize event outcome", exception);
        }
    }

    public String topic() {
        return topic;
    }
}
