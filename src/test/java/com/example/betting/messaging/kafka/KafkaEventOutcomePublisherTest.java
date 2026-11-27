package com.example.betting.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.betting.config.BettingProperties;
import com.example.betting.exception.EventPublishException;
import com.example.betting.model.EventOutcome;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class KafkaEventOutcomePublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void failedKafkaSendIsPropagatedAsEventPublishException() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafkaTemplate.send(ArgumentMatchers.<ProducerRecord<String, String>>any())).thenReturn(future);

        KafkaEventOutcomePublisher publisher = publisher(Duration.ofSeconds(1));

        assertThatThrownBy(() -> publisher.publish(new EventOutcome(1001L, "Final", 11L)))
                .isInstanceOf(EventPublishException.class)
                .hasMessageContaining("Kafka publication failed")
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(publisher.topic()).isEqualTo("event-outcomes");
    }

    @Test
    void synchronousKafkaTimeoutIsPropagatedAsEventPublishException() {
        var timeout = new org.apache.kafka.common.errors.TimeoutException("metadata unavailable");
        when(kafkaTemplate.send(ArgumentMatchers.<ProducerRecord<String, String>>any())).thenThrow(timeout);

        KafkaEventOutcomePublisher publisher = publisher(Duration.ofSeconds(1));

        assertThatThrownBy(() -> publisher.publish(new EventOutcome(1001L, "Final", 11L)))
                .isInstanceOf(EventPublishException.class)
                .hasCause(timeout);
    }

    @Test
    void incompleteAcknowledgementTimesOut() {
        when(kafkaTemplate.send(ArgumentMatchers.<ProducerRecord<String, String>>any()))
                .thenReturn(new CompletableFuture<>());

        KafkaEventOutcomePublisher publisher = publisher(Duration.ofMillis(1));

        assertThatThrownBy(() -> publisher.publish(new EventOutcome(1001L, "Final", 11L)))
                .isInstanceOf(EventPublishException.class)
                .hasCauseInstanceOf(TimeoutException.class);
    }

    private KafkaEventOutcomePublisher publisher(Duration publishTimeout) {
        var properties = new BettingProperties(
                "external",
                new BettingProperties.Kafka("event-outcomes", "test-group", publishTimeout),
                new BettingProperties.RocketMq("localhost:8081", "bet-settlements", "test-group", false));
        return new KafkaEventOutcomePublisher(kafkaTemplate, new ObjectMapper(), properties);
    }
}
