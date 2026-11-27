package com.example.betting.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.betting.messaging.MessagePublisher;
import com.example.betting.model.Bet;
import com.example.betting.model.BetSettlement;
import com.example.betting.model.EventOutcome;
import com.example.betting.repository.BetRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BetSettlementServiceTest {

    @Mock
    private BetRepository repository;

    @Mock
    private MessagePublisher publisher;

    @Captor
    private ArgumentCaptor<BetSettlement> settlementCaptor;

    @Test
    void publishesSettlementForBetsMatchingEventAndWinner() {
        when(repository.findByEventId(1001L)).thenReturn(List.of(
                bet(1L, 11L),
                bet(2L, 12L)));

        int count = new BetSettlementService(repository, publisher)
                .settleMatchingBets(new EventOutcome(1001L, "Team A vs Team B", 11L));

        org.junit.jupiter.api.Assertions.assertEquals(1, count);
        verify(publisher, times(1)).publishBetSettlement(settlementCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1L, settlementCaptor.getValue().betId());
        org.junit.jupiter.api.Assertions.assertEquals("Team A vs Team B", settlementCaptor.getValue().eventName());
    }

    @Test
    void doesNotPublishSettlementWhenNoBetMatchesWinner() {
        when(repository.findByEventId(1001L)).thenReturn(List.of(bet(1L, 11L)));

        int count = new BetSettlementService(repository, publisher)
                .settleMatchingBets(new EventOutcome(1001L, "Team A vs Team B", 99L));

        org.junit.jupiter.api.Assertions.assertEquals(0, count);
        verify(publisher, never()).publishBetSettlement(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotPublishSettlementWhenEventHasNoBets() {
        when(repository.findByEventId(9999L)).thenReturn(List.of());

        int count = new BetSettlementService(repository, publisher)
                .settleMatchingBets(new EventOutcome(9999L, "No bets event", 11L));

        org.junit.jupiter.api.Assertions.assertEquals(0, count);
        verify(publisher, never()).publishBetSettlement(org.mockito.ArgumentMatchers.any());
    }

    private static Bet bet(long betId, long winnerId) {
        return new Bet(betId, 100L + betId, 1001L, "MATCH_WINNER", winnerId, new BigDecimal("10.00"));
    }
}
