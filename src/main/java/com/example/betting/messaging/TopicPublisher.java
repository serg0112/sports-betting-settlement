package com.example.betting.messaging;

/** Single-topic publishing boundary implemented by Kafka and RocketMQ adapters. */
public interface TopicPublisher<T> {

    void publish(T message);
}
