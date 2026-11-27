package com.example.betting.model;

/** Acknowledgement returned by the publish endpoint. */
public record PublishedEventOutcome(String topic, long eventId, String status) {
}
