package com.example.betting.messaging.rocketmq;

import com.example.betting.messaging.InboundMessageValidator;
import com.example.betting.model.BetSettlement;
import com.example.betting.service.BetSettlementService;
import java.nio.ByteBuffer;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "external")
public class RocketMqSettlementListener {

    private static final Logger log = LoggerFactory.getLogger(RocketMqSettlementListener.class);

    private final ObjectMapper mapper;
    private final InboundMessageValidator validator;
    private final BetSettlementService service;

    public RocketMqSettlementListener(
            ObjectMapper mapper,
            InboundMessageValidator validator,
            BetSettlementService service) {
        this.mapper = mapper;
        this.validator = validator;
        this.service = service;
    }

    public ConsumeResult consume(MessageView message) {
        try {
            ByteBuffer body = message.getBody();
            byte[] bytes = new byte[body.remaining()];
            body.get(bytes);
            BetSettlement settlement = mapper.readValue(bytes, BetSettlement.class);
            service.settle(validator.validate(settlement, "Bet settlement"));
            return ConsumeResult.SUCCESS;
        } catch (JacksonException exception) {
            log.error("Unable to deserialize RocketMQ settlement message {}", message.getMessageId(), exception);
            return ConsumeResult.FAILURE;
        } catch (Exception exception) {
            log.error("Unable to consume RocketMQ settlement message {}", message.getMessageId(), exception);
            return ConsumeResult.FAILURE;
        }
    }
}
