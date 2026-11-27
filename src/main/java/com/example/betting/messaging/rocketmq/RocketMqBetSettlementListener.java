package com.example.betting.messaging.rocketmq;

import com.example.betting.config.BettingProperties;
import com.example.betting.messaging.BetSettlementListener;
import com.example.betting.model.BetSettlement;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** RocketMQ consumer adapter that registers the settlement handler. */
@Component
public class RocketMqBetSettlementListener implements BetSettlementListener {

    private final RocketMqSettlementMessageHandler messageHandler;

    public RocketMqBetSettlementListener(RocketMqSettlementMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void start(Consumer<BetSettlement> handler) {
        messageHandler.register(handler);
    }
}
