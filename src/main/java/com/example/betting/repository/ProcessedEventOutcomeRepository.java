package com.example.betting.repository;

import com.example.betting.model.ProcessedEventOutcome;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventOutcomeRepository extends CrudRepository<ProcessedEventOutcome, Long> {

    @Modifying
    @Query("""
            INSERT INTO PROCESSED_EVENT_OUTCOMES (EVENT_ID, EVENT_NAME, EVENT_WINNER_ID)
            SELECT :eventId, :eventName, :eventWinnerId
            WHERE NOT EXISTS (
                SELECT 1 FROM PROCESSED_EVENT_OUTCOMES WHERE EVENT_ID = :eventId
            )
            """)
    int registerIfAbsent(
            @Param("eventId") long eventId,
            @Param("eventName") String eventName,
            @Param("eventWinnerId") long eventWinnerId);
}
