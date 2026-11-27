package com.example.betting.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.betting.messaging.embedded.EmbeddedMessageBroker;
import com.example.betting.model.EventOutcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventOutcomeControllerIntegrationTest {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private EmbeddedMessageBroker broker;

    @LocalServerPort
    private int port;

    @Test
    void publishedOutcomeSettlesMatchingBetsThroughBothTopics() throws Exception {
        CopyOnWriteArrayList<String> settlements = new CopyOnWriteArrayList<>();
        broker.subscribe("bet-settlements", settlements::add);

        ResponseEntity<String> response = client().post()
                .uri(url())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"eventId\":1001,\"eventName\":\"Team A vs Team B\",\"eventWinnerId\":11}")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(response.getBody()).contains("\"topic\":\"event-outcomes\"", "\"status\":\"PUBLISHED\"");

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (settlements.isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(settlements).singleElement().satisfies(payload ->
                assertThat(payload).contains("\"betId\":1", "\"userId\":101", "\"eventWinnerId\":11"));
    }

    @Test
    void invalidRequestReturnsProblemDetailWithFieldErrors() {
        ResponseEntity<String> response = client().post()
                .uri(url())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"eventId\":0,\"eventName\":\"\",\"eventWinnerId\":null}")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Invalid request", "eventId", "eventName", "eventWinnerId");
    }

    @Test
    void outcomeWithNoMatchingBetProducesNoSettlement() throws Exception {
        CopyOnWriteArrayList<String> settlements = new CopyOnWriteArrayList<>();
        broker.subscribe("bet-settlements", settlements::add);

        EventOutcome outcome = new EventOutcome(7777L, "No bets", 12L);
        ResponseEntity<String> response = client().post()
                .uri(url())
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapper.writeValueAsString(outcome))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        Thread.sleep(100);
        assertThat(settlements).isEmpty();
    }

    private RestClient client() {
        return RestClient.builder()
                .defaultStatusHandler(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response) -> {
                })
                .build();
    }

    private URI url() {
        return URI.create("http://localhost:" + port + "/api/event-outcomes");
    }
}
