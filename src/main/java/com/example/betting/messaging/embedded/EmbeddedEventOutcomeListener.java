package com.example.betting.messaging.embedded;

import com.example.betting.config.BettingProperties;
import com.example.betting.model.EventOutcome;
import com.example.betting.service.BetSettlementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "embedded")
public class EmbeddedEventOutcomeListener {

    public EmbeddedEventOutcomeListener(
            EmbeddedMessageBroker broker,
            ObjectMapper mapper,
            BettingProperties properties,
            BetSettlementService service) {
        broker.subscribe(properties.kafka().eventOutcomesTopic(), payload -> {
            try {
                service.settleMatchingBets(mapper.readValue(payload, EventOutcome.class));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Unable to deserialize event outcome", exception);
            }
        });
    }
}
