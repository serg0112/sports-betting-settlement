package com.example.betting.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.betting.model.EventOutcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(
        properties = {
            "betting.broker-mode=embedded",
            "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
        })
@EmbeddedKafka(partitions = 1, topics = "event-outcomes")
class KafkaEventOutcomeFlowTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void producesAndConsumesEventOutcomeFromNamedKafkaTopic() throws Exception {
        Map<String, Object> producerProps = new java.util.HashMap<>(KafkaTestUtils.producerProps(embeddedKafka));
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        KafkaTemplate<String, String> template = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(producerProps));

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("kafka-flow-test", "false", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        var consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "event-outcomes");

        EventOutcome outcome = new EventOutcome(4242L, "Kafka event", 77L);
        template.send("event-outcomes", "4242", mapper.writeValueAsString(outcome)).get();
        template.flush();

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "event-outcomes", Duration.ofSeconds(10));
        assertThat(record.key()).isEqualTo("4242");
        assertThat(mapper.readValue(record.value(), EventOutcome.class)).isEqualTo(outcome);

        consumer.close();
        template.destroy();
    }
}
