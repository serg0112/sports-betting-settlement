package com.example.betting.messaging;

import com.example.betting.model.BetSettlement;

public interface BetSettlementPublisher {

    void publish(BetSettlement settlement);
}
