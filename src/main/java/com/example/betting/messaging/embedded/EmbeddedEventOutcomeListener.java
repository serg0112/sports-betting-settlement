package com.example.betting.messaging.embedded;

import com.example.betting.config.BettingTopics;
import com.example.betting.messaging.EventOutcomeListener;
import com.example.betting.model.EventOutcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.Consumer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class EmbeddedEventOutcomeListener implements EventOutcomeListener {

    private final EmbeddedMessageBroker broker;
    private final ObjectMapper mapper;

    public EmbeddedEventOutcomeListener(EmbeddedMessageBroker broker, ObjectMapper mapper) {
        this.broker = broker;
        this.mapper = mapper;
    }

    @Override
    public void start(Consumer<EventOutcome> handler) {
        broker.subscribe(BettingTopics.EVENT_OUTCOMES, payload -> {
            try {
                handler.accept(mapper.readValue(payload, EventOutcome.class));
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to deserialize event outcome", exception);
            }
        });
    }
}
