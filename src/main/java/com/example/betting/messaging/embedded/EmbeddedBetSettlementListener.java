package com.example.betting.messaging.embedded;

import com.example.betting.config.BettingTopics;
import com.example.betting.messaging.BetSettlementListener;
import com.example.betting.model.BetSettlement;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.Consumer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class EmbeddedBetSettlementListener implements BetSettlementListener {

    private final EmbeddedMessageBroker broker;
    private final ObjectMapper mapper;

    public EmbeddedBetSettlementListener(EmbeddedMessageBroker broker, ObjectMapper mapper) {
        this.broker = broker;
        this.mapper = mapper;
    }

    @Override
    public void start(Consumer<BetSettlement> handler) {
        broker.subscribe(BettingTopics.BET_SETTLEMENTS, payload -> {
            try {
                handler.accept(mapper.readValue(payload, BetSettlement.class));
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to deserialize bet settlement", exception);
            }
        });
    }
}
