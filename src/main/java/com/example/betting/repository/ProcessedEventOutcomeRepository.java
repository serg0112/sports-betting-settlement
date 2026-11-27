package com.example.betting.repository;

import com.example.betting.model.ProcessedEventOutcome;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventOutcomeRepository extends CrudRepository<ProcessedEventOutcome, Long> {

    default int registerIfAbsent(
            long eventId, String eventName, long eventWinnerId) {
        try {
            return insert(eventId, eventName, eventWinnerId);
        } catch (DuplicateKeyException exception) {
            return 0;
        }
    }

    @Modifying
    @Query("""
            INSERT INTO processed_event_outcome (event_id, event_name, event_winner_id)
            VALUES (:eventId, :eventName, :eventWinnerId)
            """)
    int insert(
            @Param("eventId") long eventId,
            @Param("eventName") String eventName,
            @Param("eventWinnerId") long eventWinnerId);
}
