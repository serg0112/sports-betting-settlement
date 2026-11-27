package com.example.betting.messaging.kafka;

import com.example.betting.model.EventOutcome;
import com.example.betting.service.BetSettlementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "external")
public class KafkaEventOutcomeListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventOutcomeListener.class);

    private final ObjectMapper mapper;
    private final BetSettlementService service;

    public KafkaEventOutcomeListener(ObjectMapper mapper, BetSettlementService service) {
        this.mapper = mapper;
        this.service = service;
    }

    @KafkaListener(
            topics = "${betting.kafka.event-outcomes-topic}",
            groupId = "${betting.kafka.consumer-group}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            service.settleMatchingBets(mapper.readValue(record.value(), EventOutcome.class));
        } catch (JsonProcessingException exception) {
            log.error("Unable to deserialize event outcome from Kafka record key={}", record.key(), exception);
            throw new IllegalStateException("Unable to deserialize event outcome from Kafka", exception);
        }
    }
}
