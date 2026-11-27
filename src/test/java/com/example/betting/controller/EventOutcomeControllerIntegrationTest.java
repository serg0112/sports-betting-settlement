package com.example.betting.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.betting.model.BetStatus;
import com.example.betting.repository.BetRepository;
import java.net.URI;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = {"/schema.sql", "/data.sql"})
class EventOutcomeControllerIntegrationTest {

    @Autowired
    private BetRepository betRepository;

    @LocalServerPort
    private int port;

    @Test
    void publishedOutcomePersistsWonAndLostStatuses() {
        ResponseEntity<String> response = post(
                "{\"eventId\":1001,\"eventName\":\"Team A vs Team B\",\"eventWinnerId\":11}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(response.getBody()).contains("\"topic\":\"event-outcomes\"", "\"status\":\"PUBLISHED\"");

        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(statusOf(1L)).isEqualTo(BetStatus.WON);
            assertThat(statusOf(2L)).isEqualTo(BetStatus.LOST);
            assertThat(statusOf(3L)).isEqualTo(BetStatus.PENDING);
        });
    }

    @Test
    void invalidRequestReturnsProblemDetailWithFieldErrors() {
        ResponseEntity<String> response = post(
                "{\"eventId\":0,\"eventName\":\"\",\"eventWinnerId\":null}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Invalid request", "eventId", "eventName", "eventWinnerId");
    }

    @Test
    void malformedJsonReturnsBadRequest() {
        ResponseEntity<String> response = post("{not-json");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Invalid request", "Malformed or invalid request");
    }

    @Test
    void unsupportedHttpMethodReturnsMethodNotAllowed() {
        ResponseEntity<String> response = client().get()
                .uri(URI.create("http://localhost:" + port + "/api/event-outcomes"))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).contains("Invalid request", "The request could not be processed");
    }

    @Test
    void unsupportedContentTypeReturnsUnsupportedMediaType() {
        ResponseEntity<String> response = client().post()
                .uri(URI.create("http://localhost:" + port + "/api/event-outcomes"))
                .contentType(MediaType.TEXT_PLAIN)
                .body("not-json")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).contains("Invalid request", "The request could not be processed");
    }

    @Test
    void outcomeForUnknownEventDoesNotChangeBets() {
        ResponseEntity<String> response = post(
                "{\"eventId\":7777,\"eventName\":\"No bets\",\"eventWinnerId\":11}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(statusOf(1L)).isEqualTo(BetStatus.PENDING);
        assertThat(statusOf(2L)).isEqualTo(BetStatus.PENDING);
        assertThat(statusOf(3L)).isEqualTo(BetStatus.PENDING);
    }

    @Test
    void h2ConsoleIsAvailable() {
        ResponseEntity<String> redirect = client().get()
                .uri(URI.create("http://localhost:" + port + "/h2-console"))
                .retrieve()
                .toEntity(String.class);

        assertThat(redirect.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(redirect.getHeaders().getLocation().toString()).endsWith("/h2-console/");

        ResponseEntity<String> response = client().get()
                .uri(redirect.getHeaders().getLocation())
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unknownPathReturnsNotFoundProblemDetail() {
        ResponseEntity<String> response = client().get()
                .uri(URI.create("http://localhost:" + port + "/does-not-exist"))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Invalid request", "The request could not be processed");
    }

    private ResponseEntity<String> post(String body) {
        return client().post()
                .uri(URI.create("http://localhost:" + port + "/api/event-outcomes"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    private BetStatus statusOf(long betId) {
        return betRepository.findById(betId).orElseThrow().status();
    }

    private static RestClient client() {
        return RestClient.builder()
                .defaultStatusHandler(status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, response) -> {
                        })
                .build();
    }
}
