package com.example.betting.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.betting.model.Bet;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryBetRepositoryTest {

    @Test
    void findsBetsByEventIdAndIgnoresDuplicateBetIds() {
        InMemoryBetRepository repository = new InMemoryBetRepository();
        Bet duplicate = new Bet(1L, 999L, 1001L, "MATCH_WINNER", 99L, new BigDecimal("1.00"));
        Bet newBet = new Bet(4L, 104L, 1001L, "MATCH_WINNER", 11L, new BigDecimal("5.00"));

        repository.add(List.of(duplicate, newBet));

        List<Bet> bets = repository.findByEventId(1001L);
        assertEquals(3, bets.size());
        assertEquals(List.of(1L, 2L, 4L), bets.stream().map(Bet::betId).toList());
        assertEquals(101L, bets.getFirst().userId(), "duplicate must not replace existing bet");
    }

    @Test
    void returnsEmptyListForUnknownEvent() {
        assertTrue(new InMemoryBetRepository().findByEventId(9999L).isEmpty());
    }
}
