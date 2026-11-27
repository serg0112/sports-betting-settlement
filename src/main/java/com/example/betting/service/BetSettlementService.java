package com.example.betting.service;

import com.example.betting.exception.BetNotFoundException;
import com.example.betting.exception.SettlementConflictException;
import com.example.betting.messaging.BetSettlementPublisher;
import com.example.betting.model.Bet;
import com.example.betting.model.BetSettlement;
import com.example.betting.model.BetStatus;
import com.example.betting.model.EventOutcome;
import com.example.betting.repository.BetRepository;
import com.example.betting.repository.ProcessedEventOutcomeRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional(readOnly = true)
public class BetSettlementService {

    private static final Logger log = LoggerFactory.getLogger(BetSettlementService.class);

    private final BetRepository betRepository;
    private final ProcessedEventOutcomeRepository processedOutcomes;
    private final BetSettlementPublisher settlementPublisher;

    public BetSettlementService(
            BetRepository betRepository,
            ProcessedEventOutcomeRepository processedOutcomes,
            BetSettlementPublisher settlementPublisher) {
        this.betRepository = betRepository;
        this.processedOutcomes = processedOutcomes;
        this.settlementPublisher = settlementPublisher;
    }

    @Transactional
    public int settleMatchingBets(EventOutcome outcome) {
        int registered = processedOutcomes.registerIfAbsent(
                outcome.eventId(), outcome.eventName(), outcome.eventWinnerId());
        if (registered == 0) {
            logDuplicateOutcome(outcome);
            return 0;
        }

        List<BetSettlement> settlements = betRepository
                .findByEventIdAndStatus(outcome.eventId(), BetStatus.PENDING)
                .stream()
                .map(bet -> BetSettlement.from(bet, outcome))
                .toList();

        settlements.forEach(settlementPublisher::publish);
        log.info("Published {} settlement(s) for eventId={} winnerId={}",
                settlements.size(), outcome.eventId(), outcome.eventWinnerId());
        return settlements.size();
    }

    @Transactional
    public void settle(BetSettlement settlement) {
        Bet bet = requireBet(settlement.betId());
        requireMatchingEvent(bet, settlement);
        BetStatus resultingStatus = bet.statusFor(settlement.actualWinnerId());

        if (bet.status() == resultingStatus) {
            log.info("Duplicate settlement ignored: betId={} status={}", bet.betId(), resultingStatus);
            return;
        }
        if (bet.status() != BetStatus.PENDING) {
            throw new SettlementConflictException(bet.betId(), bet.status(), resultingStatus);
        }

        int updated = betRepository.settlePending(bet.betId(), resultingStatus.name());
        if (updated == 1) {
            logAfterCommit("Settled betId={} eventId={} status={}", bet.betId(), bet.eventId(), resultingStatus);
            return;
        }

        Bet current = requireBet(bet.betId());
        if (current.status() == resultingStatus) {
            log.info("Concurrent duplicate settlement ignored: betId={} status={}", bet.betId(), resultingStatus);
            return;
        }
        throw new SettlementConflictException(bet.betId(), current.status(), resultingStatus);
    }

    private Bet requireBet(long betId) {
        return betRepository.findById(betId)
                .orElseThrow(() -> new BetNotFoundException(betId));
    }

    private void requireMatchingEvent(Bet bet, BetSettlement settlement) {
        if (bet.eventId() != settlement.eventId()) {
            throw new SettlementConflictException(bet.betId(), bet.eventId(), settlement.eventId());
        }
    }

    private void logDuplicateOutcome(EventOutcome outcome) {
        processedOutcomes.findById(outcome.eventId()).ifPresentOrElse(first -> {
            if (first.eventWinnerId() == outcome.eventWinnerId()) {
                log.info("Duplicate event outcome ignored: eventId={} winnerId={}",
                        outcome.eventId(), outcome.eventWinnerId());
            } else {
                log.warn("Conflicting event outcome ignored: eventId={} rejectedWinnerId={} firstWinnerId={}",
                        outcome.eventId(), outcome.eventWinnerId(), first.eventWinnerId());
            }
        }, () -> log.info("Duplicate event outcome ignored: eventId={}", outcome.eventId()));
    }

    private static void logAfterCommit(String message, Object... arguments) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.info(message, arguments);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info(message, arguments);
            }
        });
    }
}
