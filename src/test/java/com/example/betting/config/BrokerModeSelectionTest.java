package com.example.betting.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.betting.messaging.BetSettlementPublisher;
import com.example.betting.messaging.EventOutcomePublisher;
import com.example.betting.messaging.embedded.EmbeddedBetSettlementPublisher;
import com.example.betting.messaging.embedded.EmbeddedEventOutcomePublisher;
import com.example.betting.messaging.embedded.EmbeddedMessageBroker;
import com.example.betting.messaging.kafka.KafkaEventOutcomeListener;
import com.example.betting.messaging.kafka.KafkaEventOutcomePublisher;
import com.example.betting.messaging.rocketmq.RocketMqSettlementListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BrokerModeSelectionTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void embeddedModeSelectsOnlyEmbeddedAdapters() {
        assertThat(context.getBean(EventOutcomePublisher.class))
                .isInstanceOf(EmbeddedEventOutcomePublisher.class);
        assertThat(context.getBean(BetSettlementPublisher.class))
                .isInstanceOf(EmbeddedBetSettlementPublisher.class);
        assertThat(context.getBeansOfType(EmbeddedMessageBroker.class)).hasSize(1);
        assertThat(context.getBeansOfType(KafkaEventOutcomePublisher.class)).isEmpty();
        assertThat(context.getBeansOfType(KafkaEventOutcomeListener.class)).isEmpty();
        assertThat(context.getBeansOfType(RocketMqSettlementListener.class)).isEmpty();
    }
}
