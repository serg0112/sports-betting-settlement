package com.example.betting.messaging.embedded;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "betting", name = "broker-mode", havingValue = "embedded")
public class EmbeddedMessageBroker {

    private final Map<String, List<Consumer<String>>> subscribers = new ConcurrentHashMap<>();

    public void publish(String topic, String payload) {
        subscribers.getOrDefault(topic, List.of()).forEach(subscriber -> subscriber.accept(payload));
    }

    public AutoCloseable subscribe(String topic, Consumer<String> subscriber) {
        List<Consumer<String>> topicSubscribers =
                subscribers.computeIfAbsent(topic, key -> new CopyOnWriteArrayList<>());
        topicSubscribers.add(subscriber);
        return () -> topicSubscribers.remove(subscriber);
    }
}
