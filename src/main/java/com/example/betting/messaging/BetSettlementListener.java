package com.example.betting.messaging;

import com.example.betting.model.BetSettlement;

/** RocketMQ consumer boundary for bet-settlements. */
public interface BetSettlementListener {

    void start(java.util.function.Consumer<BetSettlement> handler);
}
