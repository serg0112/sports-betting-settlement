package com.example.betting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.example.betting.exception.EventPublishException;
import com.example.betting.messaging.EventOutcomePublisher;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EventPublicationFailureTest {

    @MockitoBean
    private EventOutcomePublisher publisher;

    @LocalServerPort
    private int port;

    @Test
    void publicationFailureReturnsInternalServerError() {
        doThrow(new EventPublishException("broker failed", new RuntimeException()))
                .when(publisher).publish(any());

        ResponseEntity<String> response = client().post()
                .uri(URI.create("http://localhost:" + port + "/api/event-outcomes"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"eventId\":1001,\"eventName\":\"Final\",\"eventWinnerId\":11}")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("Publication failed", "The event could not be published");
        verify(publisher).publish(any());
    }

    private static RestClient client() {
        return RestClient.builder()
                .defaultStatusHandler(status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, response) -> {
                        })
                .build();
    }
}
