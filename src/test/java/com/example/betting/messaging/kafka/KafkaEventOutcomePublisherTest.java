package com.example.betting.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.betting.config.BettingProperties;
import com.example.betting.exception.EventPublishException;
import com.example.betting.model.EventOutcome;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaEventOutcomePublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void failedKafkaSendIsPropagatedAsEventPublishException() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        KafkaEventOutcomePublisher publisher = new KafkaEventOutcomePublisher(
                kafkaTemplate,
                new ObjectMapper(),
                properties());

        assertThatThrownBy(() -> publisher.publish(new EventOutcome(1001L, "Final", 11L)))
                .isInstanceOf(EventPublishException.class)
                .hasMessageContaining("Kafka publication failed")
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(publisher.topic()).isEqualTo("event-outcomes");
    }

    private static BettingProperties properties() {
        return new BettingProperties(
                "external",
                new BettingProperties.Kafka("event-outcomes", "test-group", Duration.ofSeconds(1)),
                new BettingProperties.RocketMq("localhost:8081", "bet-settlements", "test-group", false));
    }
}
