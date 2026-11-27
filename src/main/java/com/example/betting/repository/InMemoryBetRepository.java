package com.example.betting.repository;

import com.example.betting.model.Bet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** Thread-safe in-memory bet store with deterministic startup fixtures. */
@Repository
public class InMemoryBetRepository implements BetRepository {

    private final Map<Long, List<Bet>> betsByEvent = new ConcurrentHashMap<>();

    public InMemoryBetRepository() {
        add(List.of(
                bet(1L, 101L, 1001L, "MATCH_WINNER", 11L, "25.00"),
                bet(2L, 102L, 1001L, "MATCH_WINNER", 12L, "40.00"),
                bet(3L, 103L, 1002L, "OVER_2_5", 21L, "15.50")));
    }

    @Override
    public List<Bet> findByEventId(long eventId) {
        List<Bet> matching = betsByEvent.getOrDefault(eventId, List.of());
        synchronized (matching) {
            return matching.stream()
                    .sorted(Comparator.comparing(Bet::betId))
                    .toList();
        }
    }

    /** Adds bets with duplicate bet IDs ignored, so repeated calls are harmless. */
    public void add(List<Bet> bets) {
        for (Bet bet : bets) {
            List<Bet> existing = betsByEvent.computeIfAbsent(bet.eventId(), key -> new ArrayList<>());
            synchronized (existing) {
                if (existing.stream().noneMatch(candidate -> candidate.betId().equals(bet.betId()))) {
                    existing.add(bet);
                }
            }
        }
    }

    private static Bet bet(long betId, long userId, long eventId, String marketId, long winnerId, String amount) {
        return new Bet(betId, userId, eventId, marketId, winnerId, new BigDecimal(amount));
    }
}
