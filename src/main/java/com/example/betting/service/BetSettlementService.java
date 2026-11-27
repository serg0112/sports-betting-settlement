package com.example.betting.service;

import com.example.betting.messaging.MessagePublisher;
import com.example.betting.model.Bet;
import com.example.betting.model.BetSettlement;
import com.example.betting.model.EventOutcome;
import com.example.betting.repository.BetRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Business rules for matching outcomes to bets and settling them. */
@Service
public class BetSettlementService {

    private static final Logger log = LoggerFactory.getLogger(BetSettlementService.class);

    private final BetRepository repository;
    private final MessagePublisher publisher;

    public BetSettlementService(BetRepository repository, MessagePublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /** Returns the number of matching bets sent to RocketMQ. */
    public int settleMatchingBets(EventOutcome outcome) {
        List<BetSettlement> settlements = repository.findByEventId(outcome.eventId()).stream()
                .filter(bet -> bet.eventWinnerId().equals(outcome.eventWinnerId()))
                .map(bet -> BetSettlement.from(bet, outcome))
                .toList();

        settlements.forEach(settlement -> {
            publisher.publishBetSettlement(settlement);
            log.info("Settlement published for betId={} eventId={} winnerId={}",
                    settlement.betId(), settlement.eventId(), settlement.eventWinnerId());
        });
        return settlements.size();
    }

    public void settle(BetSettlement settlement) {
        log.info("Settled betId={} userId={} eventId={} winnerId={} amount={}",
                settlement.betId(), settlement.userId(), settlement.eventId(),
                settlement.eventWinnerId(), settlement.betAmount());
    }
}
