package com.example.betting.config;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "external")
public class KafkaConfiguration {

    @Bean
    DefaultKafkaProducerFactoryCustomizer kafkaProducerTimeout(BettingProperties properties) {
        long publishTimeoutMillis = properties.kafka().publishTimeout().toMillis();
        return factory -> factory.updateConfigs(Map.of(ProducerConfig.MAX_BLOCK_MS_CONFIG, publishTimeoutMillis));
    }

    @Bean
    CommonErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(0L, 9L));
    }
}
