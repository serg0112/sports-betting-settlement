package com.example.betting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.betting.exception.BetNotFoundException;
import com.example.betting.exception.SettlementConflictException;
import com.example.betting.messaging.embedded.EmbeddedMessageBroker;
import com.example.betting.model.BetSettlement;
import com.example.betting.model.BetStatus;
import com.example.betting.model.EventOutcome;
import com.example.betting.repository.BetRepository;
import com.example.betting.repository.ProcessedEventOutcomeRepository;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = {"/schema.sql", "/data.sql"})
@ExtendWith(OutputCaptureExtension.class)
class BetSettlementIntegrationTest {

    @Autowired
    private BetSettlementService service;

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private ProcessedEventOutcomeRepository processedOutcomes;

    @Autowired
    private EmbeddedMessageBroker broker;

    @Autowired
    private ObjectMapper mapper;

    private final List<AutoCloseable> subscriptions = new ArrayList<>();

    @AfterEach
    void unsubscribeTestSubscribers() throws Exception {
        for (AutoCloseable subscription : subscriptions) {
            subscription.close();
        }
        subscriptions.clear();
    }

    @Test
    void settlesEveryBetForEventAndPersistsWinnersAndLosers() throws Exception {
        List<String> settlements = recordSettlements();

        int published = service.settleMatchingBets(new EventOutcome(1001L, "Final", 11L));

        assertThat(published).isEqualTo(2);
        assertThat(settlements).hasSize(2);
        assertThat(statusOf(1L)).isEqualTo(BetStatus.WON);
        assertThat(statusOf(2L)).isEqualTo(BetStatus.LOST);
        assertThat(statusOf(3L)).isEqualTo(BetStatus.PENDING);
        BetSettlement first = mapper.readValue(settlements.getFirst(), BetSettlement.class);
        assertThat(first.predictedWinnerId()).isEqualTo(11L);
        assertThat(first.actualWinnerId()).isEqualTo(11L);
    }

    @Test
    void eventWithoutBetsProducesNoSettlementsAndChangesNothing() {
        List<String> settlements = recordSettlements();

        int published = service.settleMatchingBets(new EventOutcome(7777L, "No bets", 11L));

        assertThat(published).isZero();
        assertThat(settlements).isEmpty();
        assertThat(statusOf(1L)).isEqualTo(BetStatus.PENDING);
        assertThat(statusOf(2L)).isEqualTo(BetStatus.PENDING);
        assertThat(statusOf(3L)).isEqualTo(BetStatus.PENDING);
    }

    @Test
    void duplicateEventOutcomeProducesNoAdditionalSettlements() {
        List<String> settlements = recordSettlements();
        EventOutcome outcome = new EventOutcome(1001L, "Final", 11L);

        int first = service.settleMatchingBets(outcome);
        int second = service.settleMatchingBets(outcome);

        assertThat(first).isEqualTo(2);
        assertThat(second).isZero();
        assertThat(settlements).hasSize(2);
        assertThat(statusOf(1L)).isEqualTo(BetStatus.WON);
        assertThat(statusOf(2L)).isEqualTo(BetStatus.LOST);
    }

    @Test
    void conflictingEventOutcomeIsLoggedAndDoesNotOverrideFirstWinner(CapturedOutput output) {
        List<String> settlements = recordSettlements();

        assertThat(service.settleMatchingBets(new EventOutcome(1001L, "Final", 11L))).isEqualTo(2);
        int rejected = service.settleMatchingBets(new EventOutcome(1001L, "Final", 12L));

        assertThat(rejected).isZero();
        assertThat(settlements).hasSize(2);
        assertThat(processedOutcomes.findById(1001L).orElseThrow().eventWinnerId()).isEqualTo(11L);
        assertThat(statusOf(1L)).isEqualTo(BetStatus.WON);
        assertThat(statusOf(2L)).isEqualTo(BetStatus.LOST);
        assertThat(output.getAll()).contains(
                "Conflicting event outcome ignored: eventId=1001 rejectedWinnerId=12 firstWinnerId=11");
    }

    @Test
    void concurrentDuplicateOutcomesRegisterOnlyOnce() throws Exception {
        List<String> settlements = recordSettlements();
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> settleWhenStarted(
                    start, new EventOutcome(1001L, "Final", 11L)));
            Future<Integer> second = executor.submit(() -> settleWhenStarted(
                    start, new EventOutcome(1001L, "Different name", 11L)));

            assertThat(List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(0, 2);
        } finally {
            executor.shutdownNow();
        }

        assertThat(processedOutcomes.count()).isEqualTo(1);
        assertThat(settlements).hasSize(2);
        assertThat(statusOf(1L)).isEqualTo(BetStatus.WON);
        assertThat(statusOf(2L)).isEqualTo(BetStatus.LOST);
    }

    @Test
    void duplicateSettlementMessageIsIdempotent() throws Exception {
        List<String> settlements = recordSettlements();
        service.settleMatchingBets(new EventOutcome(1001L, "Final", 11L));
        BetSettlement settlement = mapper.readValue(
                settlements.stream().filter(payload -> payload.contains("\"betId\":1")).findFirst().orElseThrow(),
                BetSettlement.class);

        service.settle(settlement);

        assertThat(statusOf(1L)).isEqualTo(BetStatus.WON);
    }

    @Test
    void conflictingDuplicateSettlementIsRejected() {
        service.settleMatchingBets(new EventOutcome(1001L, "Final", 11L));
        BetSettlement conflicting = settlement(1L, 1001L, 11L, 12L);

        assertThatThrownBy(() -> service.settle(conflicting))
                .isInstanceOf(SettlementConflictException.class)
                .hasMessageContaining("current status WON, requested status LOST");
        assertThat(statusOf(1L)).isEqualTo(BetStatus.WON);
    }

    @Test
    void settlementWithMismatchedEventIdIsRejected() {
        BetSettlement mismatched = settlement(3L, 1001L, 21L, 21L);

        assertThatThrownBy(() -> service.settle(mismatched))
                .isInstanceOf(SettlementConflictException.class)
                .hasMessage("Conflicting settlement for betId=3: "
                        + "eventId=1001 does not match persisted eventId=1002");
        assertThat(statusOf(3L)).isEqualTo(BetStatus.PENDING);
    }

    @Test
    void nonexistentBetIsRejectedExplicitly() {
        assertThatThrownBy(() -> service.settle(settlement(999L, 1001L, 11L, 11L)))
                .isInstanceOf(BetNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void concurrentSettlementHasOnlyOneValidTransition() throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> won = executor.submit(() -> settleWhenStarted(start, settlement(3L, 1002L, 21L, 21L)));
            Future<Boolean> lost = executor.submit(() -> settleWhenStarted(start, settlement(3L, 1002L, 21L, 99L)));

            boolean wonAccepted = won.get(5, TimeUnit.SECONDS);
            boolean lostAccepted = lost.get(5, TimeUnit.SECONDS);

            assertThat(wonAccepted != lostAccepted).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(statusOf(3L)).isIn(BetStatus.WON, BetStatus.LOST);
    }

    private List<String> recordSettlements() {
        CopyOnWriteArrayList<String> settlements = new CopyOnWriteArrayList<>();
        subscriptions.add(broker.subscribe("bet-settlements", settlements::add));
        return settlements;
    }

    private BetStatus statusOf(long betId) {
        return betRepository.findById(betId).orElseThrow().status();
    }

    private int settleWhenStarted(CyclicBarrier barrier, EventOutcome outcome) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        return service.settleMatchingBets(outcome);
    }

    private boolean settleWhenStarted(CyclicBarrier barrier, BetSettlement settlement) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        try {
            service.settle(settlement);
            return true;
        } catch (SettlementConflictException exception) {
            return false;
        }
    }

    private static BetSettlement settlement(
            long betId, long eventId, long predictedWinnerId, long actualWinnerId) {
        return new BetSettlement(betId, 100L + betId, eventId, "Final", "MATCH_WINNER",
                predictedWinnerId, actualWinnerId, new BigDecimal("10.00"));
    }
}
