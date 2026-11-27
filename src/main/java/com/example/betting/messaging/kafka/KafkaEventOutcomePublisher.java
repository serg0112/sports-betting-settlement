package com.example.betting.messaging.kafka;

import com.example.betting.config.BettingProperties;
import com.example.betting.exception.EventPublishException;
import com.example.betting.messaging.EventOutcomePublisher;
import com.example.betting.model.EventOutcome;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "external")
public class KafkaEventOutcomePublisher implements EventOutcomePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;
    private final String topic;
    private final long publishTimeoutNanos;

    public KafkaEventOutcomePublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper mapper,
            BettingProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
        this.topic = properties.kafka().eventOutcomesTopic();
        this.publishTimeoutNanos = properties.kafka().publishTimeout().toNanos();
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
            long sendStartedAt = System.nanoTime();
            var sendResult = kafkaTemplate.send(
                    new ProducerRecord<>(topic, String.valueOf(outcome.eventId()), payload));
            long remainingNanos = publishTimeoutNanos - (System.nanoTime() - sendStartedAt);
            sendResult.get(Math.max(0, remainingNanos), TimeUnit.NANOSECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EventPublishException("Interrupted while publishing event outcome", exception);
        } catch (ExecutionException | KafkaException | org.apache.kafka.common.KafkaException exception) {
            Throwable cause = exception instanceof ExecutionException && exception.getCause() != null
                    ? exception.getCause() : exception;
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
