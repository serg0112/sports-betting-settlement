package com.example.betting.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "betting")
public record BettingProperties(
        String brokerMode,
        Kafka kafka,
        RocketMq rocketmq) {

    public record Kafka(
            String eventOutcomesTopic,
            String consumerGroup,
            Duration publishTimeout) {
    }

    public record RocketMq(
            String endpoints,
            String betSettlementsTopic,
            String consumerGroup,
            boolean enableSsl) {
    }
}
