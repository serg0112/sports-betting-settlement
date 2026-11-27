package com.example.betting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.betting.exception.BetNotFoundException;
import com.example.betting.exception.SettlementConflictException;
import com.example.betting.messaging.BetSettlementPublisher;
import com.example.betting.model.Bet;
import com.example.betting.model.BetSettlement;
import com.example.betting.model.BetStatus;
import com.example.betting.model.EventOutcome;
import com.example.betting.repository.BetRepository;
import com.example.betting.repository.ProcessedEventOutcomeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BetSettlementServiceTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private ProcessedEventOutcomeRepository processedOutcomes;

    @Mock
    private BetSettlementPublisher publisher;

    private BetSettlementService service;

    @BeforeEach
    void setUp() {
        service = new BetSettlementService(betRepository, processedOutcomes, publisher);
    }

    @Test
    void publishesSettlementForEveryPendingBetAndIncludesBothWinners() {
        when(processedOutcomes.registerIfAbsent(1001L, "Final", 11L)).thenReturn(1);
        when(betRepository.findByEventIdAndStatus(1001L, BetStatus.PENDING)).thenReturn(List.of(
                bet(1L, 11L),
                bet(2L, 12L)));

        int published = service.settleMatchingBets(new EventOutcome(1001L, "Final", 11L));

        assertThat(published).isEqualTo(2);
        ArgumentCaptor<BetSettlement> captor = ArgumentCaptor.forClass(BetSettlement.class);
        verify(publisher, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(BetSettlement::betId)
                .containsExactly(1L, 2L);
        assertThat(captor.getAllValues())
                .extracting(BetSettlement::predictedWinnerId)
                .containsExactly(11L, 12L);
        assertThat(captor.getAllValues())
                .allSatisfy(settlement -> assertThat(settlement.actualWinnerId()).isEqualTo(11L));
    }

    @Test
    void duplicateEventOutcomeDoesNotPublishAgain() {
        when(processedOutcomes.registerIfAbsent(1001L, "Final", 11L)).thenReturn(0);

        int published = service.settleMatchingBets(new EventOutcome(1001L, "Final", 11L));

        assertThat(published).isZero();
        verify(betRepository, never()).findByEventIdAndStatus(anyLong(), any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void settlementResultIsDerivedFromPersistedBet() {
        BetSettlement settlement = settlement(1L, 99L, 11L);
        when(betRepository.findById(1L)).thenReturn(Optional.of(bet(1L, 11L)));
        when(betRepository.settlePending(1L, "WON")).thenReturn(1);

        service.settle(settlement);

        verify(betRepository).settlePending(1L, "WON");
    }

    @Test
    void duplicateSettlementForSameStatusIsSuccessfulNoOp() {
        BetSettlement settlement = settlement(1L, 11L, 11L);
        when(betRepository.findById(1L)).thenReturn(Optional.of(bet(1L, 11L).withStatus(BetStatus.WON)));

        service.settle(settlement);

        verify(betRepository, never()).settlePending(anyLong(), anyString());
    }

    @Test
    void conflictingDuplicateSettlementFails() {
        BetSettlement settlement = settlement(1L, 11L, 12L);
        when(betRepository.findById(1L)).thenReturn(Optional.of(bet(1L, 11L).withStatus(BetStatus.WON)));

        assertThatThrownBy(() -> service.settle(settlement))
                .isInstanceOf(SettlementConflictException.class)
                .hasMessageContaining("current status WON, requested status LOST");
        verify(betRepository, never()).settlePending(anyLong(), anyString());
    }

    @Test
    void missingBetFailsExplicitly() {
        BetSettlement settlement = settlement(999L, 11L, 11L);
        when(betRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.settle(settlement))
                .isInstanceOf(BetNotFoundException.class)
                .hasMessageContaining("999");
    }

    private static Bet bet(long betId, long predictedWinnerId) {
        return new Bet(betId, 100L + betId, 1001L, "MATCH_WINNER", predictedWinnerId,
                new BigDecimal("10.00"), BetStatus.PENDING);
    }

    private static BetSettlement settlement(long betId, long predictedWinnerId, long actualWinnerId) {
        return new BetSettlement(betId, 100L + betId, 1001L, "Final", "MATCH_WINNER",
                predictedWinnerId, actualWinnerId, new BigDecimal("10.00"));
    }
}
