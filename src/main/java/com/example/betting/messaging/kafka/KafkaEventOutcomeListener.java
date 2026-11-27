package com.example.betting.messaging.kafka;

import com.example.betting.exception.MessageValidationException;
import com.example.betting.messaging.InboundMessageValidator;
import com.example.betting.model.EventOutcome;
import com.example.betting.service.BetSettlementService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "external")
public class KafkaEventOutcomeListener {

    private final ObjectMapper mapper;
    private final InboundMessageValidator validator;
    private final BetSettlementService service;

    public KafkaEventOutcomeListener(
            ObjectMapper mapper,
            InboundMessageValidator validator,
            BetSettlementService service) {
        this.mapper = mapper;
        this.validator = validator;
        this.service = service;
    }

    @KafkaListener(
            topics = "${betting.kafka.event-outcomes-topic}",
            groupId = "${betting.kafka.consumer-group}")
    public void consume(ConsumerRecord<String, String> record) {
        EventOutcome outcome;
        try {
            outcome = mapper.readValue(record.value(), EventOutcome.class);
        } catch (JacksonException exception) {
            throw new MessageValidationException(
                    "Unable to deserialize event outcome from Kafka record key=" + record.key(), exception);
        }
        service.settleMatchingBets(validator.validate(outcome, "Event outcome"));
    }
}
