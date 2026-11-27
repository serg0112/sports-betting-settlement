package com.example.betting.messaging.rocketmq;

import com.example.betting.model.BetSettlement;
import com.example.betting.service.BetSettlementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "external")
public class RocketMqSettlementListener {

    private static final Logger log = LoggerFactory.getLogger(RocketMqSettlementListener.class);

    private final ObjectMapper mapper;
    private final BetSettlementService service;

    public RocketMqSettlementListener(ObjectMapper mapper, BetSettlementService service) {
        this.mapper = mapper;
        this.service = service;
    }

    public ConsumeResult consume(MessageView message) {
        try {
            ByteBuffer body = message.getBody();
            byte[] bytes = new byte[body.remaining()];
            body.get(bytes);
            service.settle(mapper.readValue(bytes, BetSettlement.class));
            return ConsumeResult.SUCCESS;
        } catch (Exception exception) {
            log.error("Unable to consume RocketMQ settlement message {}", message.getMessageId(), exception);
            return ConsumeResult.FAILURE;
        }
    }
}
