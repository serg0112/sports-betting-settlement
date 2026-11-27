package com.example.betting.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.betting.messaging.BetSettlementPublisher;
import com.example.betting.messaging.EventOutcomePublisher;
import com.example.betting.messaging.embedded.EmbeddedMessageBroker;
import com.example.betting.messaging.kafka.KafkaEventOutcomeListener;
import com.example.betting.messaging.kafka.KafkaEventOutcomePublisher;
import com.example.betting.messaging.rocketmq.RocketMqBetSettlementPublisher;
import com.example.betting.messaging.rocketmq.RocketMqSettlementListener;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "betting.broker-mode=external",
                "betting.kafka.publish-timeout=2s",
                "spring.kafka.listener.auto-startup=false",
                "betting.rocketmq.endpoints=localhost:1"
        })
class ExternalBrokerModeSelectionTest {

    @MockitoBean(name = "rocketMqProducer")
    private Producer producer;

    @MockitoBean(name = "rocketMqPushConsumer")
    private PushConsumer pushConsumer;

    @Autowired
    private ApplicationContext context;

    @Test
    void externalModeSelectsOnlyKafkaAndRocketMqAdapters() {
        assertThat(context.getBean(EventOutcomePublisher.class))
                .isInstanceOf(KafkaEventOutcomePublisher.class);
        assertThat(context.getBean(BetSettlementPublisher.class))
                .isInstanceOf(RocketMqBetSettlementPublisher.class);
        assertThat(context.getBeansOfType(KafkaEventOutcomeListener.class)).hasSize(1);
        assertThat(context.getBeansOfType(RocketMqSettlementListener.class)).hasSize(1);
        assertThat(context.getBeansOfType(EmbeddedMessageBroker.class)).isEmpty();
        assertThat(context.getBean(CommonErrorHandler.class)).isInstanceOf(DefaultErrorHandler.class);
    }

    @Test
    void producerBlockingUsesConfiguredPublishTimeout() {
        ProducerFactory<?, ?> factory = context.getBean(ProducerFactory.class);
        assertThat(factory.getConfigurationProperties())
                .containsEntry(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2_000L);
    }
}
