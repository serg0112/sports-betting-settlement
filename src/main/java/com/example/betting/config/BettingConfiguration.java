package com.example.betting.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.betting.messaging.BrokerMode;
import com.example.betting.messaging.EventOutcomeListener;
import com.example.betting.messaging.BetSettlementListener;
import com.example.betting.messaging.MessagePublisher;
import com.example.betting.messaging.embedded.EmbeddedEventOutcomeListener;
import com.example.betting.messaging.embedded.EmbeddedBetSettlementListener;
import com.example.betting.messaging.embedded.EmbeddedMessageBroker;
import com.example.betting.messaging.embedded.EmbeddedMessagePublisher;
import com.example.betting.messaging.kafka.KafkaEventOutcomePublisher;
import com.example.betting.messaging.kafka.KafkaEventOutcomeListener;
import com.example.betting.messaging.rocketmq.RocketMqBetSettlementPublisher;
import com.example.betting.messaging.rocketmq.RocketMqBetSettlementListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/** Wires the embedded (tests/default) or external Docker-based broker adapters. */
@Configuration
@EnableConfigurationProperties(BettingProperties.class)
public class BettingConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    BrokerMode brokerMode(BettingProperties properties) {
        return BrokerMode.parse(properties.brokerMode());
    }

    @Bean
    MessagePublisher messagePublisher(
            BrokerMode brokerMode,
            @Lazy EmbeddedMessagePublisher embeddedPublisher,
            @Lazy KafkaEventOutcomePublisher kafkaPublisher,
            @Lazy RocketMqBetSettlementPublisher rocketMqPublisher) {
        return switch (brokerMode) {
            case EMBEDDED -> embeddedPublisher;
            case EXTERNAL -> new MessagePublisher.Composite(kafkaPublisher, rocketMqPublisher);
        };
    }

    @Bean
    EventOutcomeListener eventOutcomeListener(
            BrokerMode brokerMode,
            @Lazy EmbeddedEventOutcomeListener embeddedListener,
            @Lazy KafkaEventOutcomeListener kafkaListener) {
        return switch (brokerMode) {
            case EMBEDDED -> embeddedListener;
            case EXTERNAL -> kafkaListener;
        };
    }

    @Bean
    BetSettlementListener betSettlementListener(
            BrokerMode brokerMode,
            @Lazy EmbeddedBetSettlementListener embeddedListener,
            @Lazy RocketMqBetSettlementListener rocketMqListener) {
        return switch (brokerMode) {
            case EMBEDDED -> embeddedListener;
            case EXTERNAL -> rocketMqListener;
        };
    }
}
