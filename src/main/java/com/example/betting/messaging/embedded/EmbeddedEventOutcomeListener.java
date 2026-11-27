package com.example.betting.messaging.embedded;

import com.example.betting.config.BettingProperties;
import com.example.betting.exception.MessageValidationException;
import com.example.betting.messaging.InboundMessageValidator;
import com.example.betting.model.EventOutcome;
import com.example.betting.service.BetSettlementService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "embedded")
public class EmbeddedEventOutcomeListener {

    public EmbeddedEventOutcomeListener(
            EmbeddedMessageBroker broker,
            ObjectMapper mapper,
            InboundMessageValidator validator,
            BettingProperties properties,
            BetSettlementService service) {
        broker.subscribe(properties.kafka().eventOutcomesTopic(), payload -> {
            EventOutcome outcome;
            try {
                outcome = mapper.readValue(payload, EventOutcome.class);
            } catch (JacksonException exception) {
                throw new MessageValidationException("Unable to deserialize event outcome", exception);
            }
            service.settleMatchingBets(validator.validate(outcome, "Event outcome"));
        });
    }
}
