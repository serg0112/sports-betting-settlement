package com.example.betting.messaging.embedded;

import com.example.betting.config.BettingProperties;
import com.example.betting.exception.SettlementPublishException;
import com.example.betting.messaging.BetSettlementPublisher;
import com.example.betting.model.BetSettlement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "embedded")
public class EmbeddedBetSettlementPublisher implements BetSettlementPublisher {

    private final EmbeddedMessageBroker broker;
    private final ObjectMapper mapper;
    private final String topic;

    public EmbeddedBetSettlementPublisher(
            EmbeddedMessageBroker broker,
            ObjectMapper mapper,
            BettingProperties properties) {
        this.broker = broker;
        this.mapper = mapper;
        this.topic = properties.rocketmq().betSettlementsTopic();
    }

    @Override
    public void publish(BetSettlement settlement) {
        try {
            broker.publish(topic, mapper.writeValueAsString(settlement));
        } catch (JsonProcessingException exception) {
            throw new SettlementPublishException("Unable to serialize bet settlement", exception);
        }
    }
}
