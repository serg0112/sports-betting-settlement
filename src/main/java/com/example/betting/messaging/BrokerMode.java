package com.example.betting.messaging;

/** Selects embedded brokers by default and real brokers with broker-mode=external. */
public enum BrokerMode {
    EMBEDDED,
    EXTERNAL;

    public static BrokerMode parse(String value) {
        if (value == null || value.isBlank()) {
            return EMBEDDED;
        }
        return valueOf(value.trim().toUpperCase());
    }
}
