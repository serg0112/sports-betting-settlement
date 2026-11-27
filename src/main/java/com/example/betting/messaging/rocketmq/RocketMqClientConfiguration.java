package com.example.betting.messaging.rocketmq;

import com.example.betting.config.BettingProperties;
import java.util.Map;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "external")
public class RocketMqClientConfiguration {

    @Bean(destroyMethod = "close")
    Producer rocketMqProducer(BettingProperties properties) throws Exception {
        ClientConfiguration configuration = clientConfiguration(properties);
        return ClientServiceProvider.loadService().newProducerBuilder()
                .setClientConfiguration(configuration)
                .setTopics(properties.rocketmq().betSettlementsTopic())
                .build();
    }

    @Bean(destroyMethod = "close")
    PushConsumer rocketMqPushConsumer(
            BettingProperties properties,
            RocketMqSettlementListener listener) throws Exception {
        ClientConfiguration configuration = clientConfiguration(properties);
        return ClientServiceProvider.loadService().newPushConsumerBuilder()
                .setClientConfiguration(configuration)
                .setConsumerGroup(properties.rocketmq().consumerGroup())
                .setSubscriptionExpressions(Map.of(
                        properties.rocketmq().betSettlementsTopic(),
                        new FilterExpression("*", FilterExpressionType.TAG)))
                .setMessageListener(listener::consume)
                .build();
    }

    private static ClientConfiguration clientConfiguration(BettingProperties properties) {
        ClientConfigurationBuilder builder = ClientConfiguration.newBuilder()
                .setEndpoints(properties.rocketmq().endpoints());
        if (!properties.rocketmq().enableSsl()) {
            builder.enableSsl(false);
        }
        return builder.build();
    }
}
