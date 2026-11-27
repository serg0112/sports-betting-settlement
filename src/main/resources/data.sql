MERGE INTO bets (bet_id, user_id, event_id, event_market_id, event_winner_id, bet_amount, status)
KEY (bet_id) VALUES (1, 101, 1001, 'MATCH_WINNER', 11, 25.00, 'PENDING');

MERGE INTO bets (bet_id, user_id, event_id, event_market_id, event_winner_id, bet_amount, status)
KEY (bet_id) VALUES (2, 102, 1001, 'MATCH_WINNER', 12, 40.00, 'PENDING');

MERGE INTO bets (bet_id, user_id, event_id, event_market_id, event_winner_id, bet_amount, status)
KEY (bet_id) VALUES (3, 103, 1002, 'OVER_2_5', 21, 15.50, 'PENDING');
