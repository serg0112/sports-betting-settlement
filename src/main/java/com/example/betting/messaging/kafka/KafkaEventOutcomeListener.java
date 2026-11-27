package com.example.betting.messaging.kafka;

import com.example.betting.config.BettingProperties;
import com.example.betting.messaging.EventOutcomeListener;
import com.example.betting.model.EventOutcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Kafka consumer that delegates each event outcome to the settlement service. */
@Component
public class KafkaEventOutcomeListener implements EventOutcomeListener {

    private final ObjectMapper mapper;
    private final String topic;
    private Consumer<EventOutcome> handler = outcome -> {
    };

    public KafkaEventOutcomeListener(ObjectMapper mapper, BettingProperties properties) {
        this.mapper = mapper;
        this.topic = properties.kafka().eventOutcomesTopic();
    }

    @Override
    public void start(Consumer<EventOutcome> handler) {
        this.handler = handler;
    }

    @KafkaListener(
            topics = "${betting.kafka.event-outcomes-topic}",
            groupId = "${betting.kafka.consumer-group}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            handler.accept(mapper.readValue(record.value(), EventOutcome.class));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to deserialize event outcome from Kafka", exception);
        }
    }

    public String topic() {
        return topic;
    }
}
