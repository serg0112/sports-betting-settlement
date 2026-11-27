DROP TABLE IF EXISTS processed_event_outcomes;
DROP TABLE IF EXISTS bets;

CREATE TABLE bets (
    bet_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    event_market_id VARCHAR(64) NOT NULL,
    event_winner_id BIGINT NOT NULL,
    bet_amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(16) NOT NULL
);

CREATE INDEX idx_bets_event_id_status ON bets(event_id, status);

CREATE TABLE processed_event_outcomes (
    event_id BIGINT PRIMARY KEY,
    event_name VARCHAR(255) NOT NULL,
    event_winner_id BIGINT NOT NULL
);
