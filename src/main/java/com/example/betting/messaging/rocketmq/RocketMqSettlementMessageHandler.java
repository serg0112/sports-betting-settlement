package com.example.betting.messaging.rocketmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.springframework.stereotype.Component;

/** Deserializes RocketMQ messages and delegates settlement to the registered handler. */
@Component
public class RocketMqSettlementMessageHandler {

    private final ObjectMapper mapper;
    private Consumer<com.example.betting.model.BetSettlement> handler = settlement -> {
    };

    public RocketMqSettlementMessageHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void register(Consumer<com.example.betting.model.BetSettlement> handler) {
        this.handler = handler;
    }

    public ConsumeResult consume(MessageView message) {
        try {
            ByteBuffer body = message.getBody();
            byte[] bytes = new byte[body.remaining()];
            body.get(bytes);
            handler.accept(mapper.readValue(bytes, com.example.betting.model.BetSettlement.class));
            return ConsumeResult.SUCCESS;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to consume RocketMQ bet settlement", exception);
        }
    }
}
