package com.example.betting.repository;

import com.example.betting.model.Bet;
import java.util.List;

/** Persistence boundary for bets, independent of storage technology. */
public interface BetRepository {

    List<Bet> findByEventId(long eventId);
}
