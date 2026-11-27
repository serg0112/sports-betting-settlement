package com.example.betting.messaging.rocketmq;

import com.example.betting.config.BettingProperties;
import com.example.betting.exception.SettlementPublishException;
import com.example.betting.messaging.BetSettlementPublisher;
import com.example.betting.model.BetSettlement;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "external")
public class RocketMqBetSettlementPublisher implements BetSettlementPublisher {

    private static final Logger log = LoggerFactory.getLogger(RocketMqBetSettlementPublisher.class);

    private final Producer producer;
    private final ObjectMapper mapper;
    private final String topic;

    public RocketMqBetSettlementPublisher(
            Producer producer,
            ObjectMapper mapper,
            BettingProperties properties) {
        this.producer = producer;
        this.mapper = mapper;
        this.topic = properties.rocketmq().betSettlementsTopic();
    }

    @Override
    public void publish(BetSettlement settlement) {
        try {
            byte[] body = mapper.writeValueAsBytes(settlement);
            Message message = ClientServiceProvider.loadService().newMessageBuilder()
                    .setTopic(topic)
                    .setKeys(String.valueOf(settlement.betId()))
                    .setBody(body)
                    .build();
            SendReceipt receipt = producer.send(message);
            log.debug("Published settlement for betId={} messageId={}", settlement.betId(), receipt.getMessageId());
        } catch (JacksonException exception) {
            throw new SettlementPublishException("Unable to serialize bet settlement", exception);
        } catch (Exception exception) {
            throw new SettlementPublishException(
                    "Unable to publish bet settlement to RocketMQ topic " + topic, exception);
        }
    }
}
