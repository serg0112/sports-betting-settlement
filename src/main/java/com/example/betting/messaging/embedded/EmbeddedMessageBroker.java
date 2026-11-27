package com.example.betting.messaging.embedded;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** Deterministic in-process broker used by tests and the default run mode. */
@Component
public class EmbeddedMessageBroker {

    private final Map<String, List<Consumer<String>>> subscribers = new ConcurrentHashMap<>();

    public void publish(String topic, String payload) {
        subscribers.getOrDefault(topic, List.of()).forEach(subscriber -> subscriber.accept(payload));
    }

    public void subscribe(String topic, Consumer<String> subscriber) {
        subscribers.computeIfAbsent(topic, key -> new CopyOnWriteArrayList<>()).add(subscriber);
    }
}
