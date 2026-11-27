DROP TABLE IF EXISTS processed_event_outcome;
DROP TABLE IF EXISTS bet;

CREATE TABLE bet (
    bet_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    event_market_id VARCHAR(64) NOT NULL,
    event_winner_id BIGINT NOT NULL,
    bet_amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(16) NOT NULL
);

CREATE INDEX idx_bet_event_id_status ON bet(event_id, status);

CREATE TABLE processed_event_outcome (
    event_id BIGINT PRIMARY KEY,
    event_name VARCHAR(255) NOT NULL,
    event_winner_id BIGINT NOT NULL
);
