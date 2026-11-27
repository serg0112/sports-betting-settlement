package com.example.betting.controller;

import com.example.betting.model.EventOutcome;
import com.example.betting.model.PublishedEventOutcome;
import com.example.betting.service.OutcomePublishService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event-outcomes")
public class EventOutcomeController {

    private final OutcomePublishService publishService;

    public EventOutcomeController(OutcomePublishService publishService) {
        this.publishService = publishService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublishedEventOutcome publish(@Valid @RequestBody EventOutcome outcome) {
        return publishService.publish(outcome);
    }
}
