package com.example.betting.messaging.kafka;

import com.example.betting.config.BettingProperties;
import com.example.betting.exception.EventPublishException;
import com.example.betting.messaging.EventOutcomePublisher;
import com.example.betting.model.EventOutcome;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "external")
public class KafkaEventOutcomePublisher implements EventOutcomePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;
    private final String topic;
    private final long publishTimeoutMillis;

    public KafkaEventOutcomePublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper mapper,
            BettingProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
        this.topic = properties.kafka().eventOutcomesTopic();
        this.publishTimeoutMillis = properties.kafka().publishTimeout().toMillis();
    }

    @Override
    public void publish(EventOutcome outcome) {
        String payload;
        try {
            payload = mapper.writeValueAsString(outcome);
        } catch (JacksonException exception) {
            throw new EventPublishException("Unable to serialize event outcome", exception);
        }

        try {
            kafkaTemplate.send(new ProducerRecord<>(topic, String.valueOf(outcome.eventId()), payload))
                    .get(publishTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EventPublishException("Interrupted while publishing event outcome", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new EventPublishException("Kafka publication failed for topic " + topic, cause);
        } catch (TimeoutException exception) {
            throw new EventPublishException("Timed out publishing to Kafka topic " + topic, exception);
        }
    }

    @Override
    public String topic() {
        return topic;
    }
}
