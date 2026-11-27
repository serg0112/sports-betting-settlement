package com.example.betting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** External broker settings; ignored when broker-mode is embedded. */
@ConfigurationProperties(prefix = "betting")
public record BettingProperties(
        String brokerMode,
        Kafka kafka,
        RocketMq rocketmq) {

    public record Kafka(
            String bootstrapServers,
            String eventOutcomesTopic,
            String consumerGroup) {
    }

    public record RocketMq(
            String endpoints,
            String betSettlementsTopic,
            String consumerGroup,
            boolean enableSsl) {
    }
}
