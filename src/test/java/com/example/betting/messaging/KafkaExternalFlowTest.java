package com.example.betting.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.betting.messaging.rocketmq.RocketMqSettlementListener;
import com.example.betting.model.BetStatus;
import com.example.betting.repository.BetRepository;
import java.net.URI;
import java.nio.ByteBuffer;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.RestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "betting.broker-mode=external",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "betting.kafka.consumer-group=external-flow-group",
                "betting.rocketmq.endpoints=localhost:1"
        })
@EmbeddedKafka(partitions = 1, topics = "event-outcomes")
@Sql(scripts = {"/schema.sql", "/data.sql"})
class KafkaExternalFlowTest {

    @MockitoBean(name = "rocketMqProducer")
    private Producer producer;

    @MockitoBean(name = "rocketMqPushConsumer")
    private PushConsumer pushConsumer;

    @Autowired
    private RocketMqSettlementListener settlementListener;

    @Autowired
    private BetRepository betRepository;

    @LocalServerPort
    private int port;

    @Test
    void apiRequestFlowsThroughActualKafkaAdapterAndActualRocketMqPublisherToDatabase() throws Exception {
        when(producer.send(any(Message.class))).thenReturn(mock(SendReceipt.class));

        ResponseEntity<String> response = RestClient.create().post()
                .uri(URI.create("http://localhost:" + port + "/api/event-outcomes"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"eventId\":1001,\"eventName\":\"Final\",\"eventWinnerId\":11}")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
        verify(producer, timeout(10_000).times(2)).send(messages.capture());

        for (Message message : messages.getAllValues()) {
            MessageView view = mock(MessageView.class);
            when(view.getBody()).thenReturn(ByteBuffer.wrap(bodyOf(message)));
            assertThat(settlementListener.consume(view)).isEqualTo(ConsumeResult.SUCCESS);
        }

        assertThat(statusOf(1L)).isEqualTo(BetStatus.WON);
        assertThat(statusOf(2L)).isEqualTo(BetStatus.LOST);
        assertThat(statusOf(3L)).isEqualTo(BetStatus.PENDING);
    }

    private BetStatus statusOf(long betId) {
        return betRepository.findById(betId).orElseThrow().status();
    }

    private static byte[] bodyOf(Message message) {
        ByteBuffer buffer = message.getBody().duplicate();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
