package com.example.betting.messaging.rocketmq;

import com.example.betting.config.BettingProperties;
import com.example.betting.messaging.TopicPublisher;
import com.example.betting.model.BetSettlement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** RocketMQ producer for the bet-settlements named topic. */
@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "external")
public class RocketMqBetSettlementPublisher implements TopicPublisher<BetSettlement> {

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
            producer.send(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize bet settlement", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to publish bet settlement to RocketMQ", exception);
        }
    }

    public String topic() {
        return topic;
    }
}
