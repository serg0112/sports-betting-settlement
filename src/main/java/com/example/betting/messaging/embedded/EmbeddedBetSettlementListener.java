package com.example.betting.messaging.embedded;

import com.example.betting.config.BettingProperties;
import com.example.betting.model.BetSettlement;
import com.example.betting.service.BetSettlementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "embedded")
public class EmbeddedBetSettlementListener {

    public EmbeddedBetSettlementListener(
            EmbeddedMessageBroker broker,
            ObjectMapper mapper,
            BettingProperties properties,
            BetSettlementService service) {
        broker.subscribe(properties.rocketmq().betSettlementsTopic(), payload -> {
            try {
                service.settle(mapper.readValue(payload, BetSettlement.class));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Unable to deserialize bet settlement", exception);
            }
        });
    }
}
