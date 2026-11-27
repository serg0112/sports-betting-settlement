package com.example.betting.startup;

import com.example.betting.messaging.BetSettlementListener;
import com.example.betting.messaging.EventOutcomeListener;
import com.example.betting.service.BetSettlementService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Connects both consumers to the settlement service on application startup. */
@Component
public class SettlementListenersInitializer implements ApplicationRunner {

    private final EventOutcomeListener outcomeListener;
    private final BetSettlementListener settlementListener;
    private final BetSettlementService service;

    public SettlementListenersInitializer(
            EventOutcomeListener outcomeListener,
            BetSettlementListener settlementListener,
            BetSettlementService service) {
        this.outcomeListener = outcomeListener;
        this.settlementListener = settlementListener;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        outcomeListener.start(service::settleMatchingBets);
        settlementListener.start(service::settle);
    }
}
