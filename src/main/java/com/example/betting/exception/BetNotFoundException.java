package com.example.betting.exception;

public class BetNotFoundException extends RuntimeException {

    public BetNotFoundException(long betId) {
        super("Bet not found: betId=" + betId);
    }
}
