package com.example.betting.repository;

import com.example.betting.model.Bet;
import com.example.betting.model.BetStatus;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface BetRepository extends CrudRepository<Bet, Long> {

    List<Bet> findByEventIdAndStatus(long eventId, BetStatus status);

    @Modifying
    @Query("UPDATE BETS SET STATUS = :status WHERE BET_ID = :betId AND STATUS = 'PENDING'")
    int settlePending(@Param("betId") long betId, @Param("status") String status);
}
