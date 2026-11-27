# Sports Betting Settlement Trigger Service

Spring Boot service that publishes sports event outcomes to Kafka, turns pending bets into RocketMQ settlement messages, and persists bet settlement results in an in-memory H2 database.

## Architecture and flow

1. `POST /api/event-outcomes` validates the outcome and publishes it to the Kafka topic `event-outcomes`.
2. The Kafka consumer loads every **PENDING** bet for the outcome's `eventId` and publishes one `bet-settlements` RocketMQ message per bet.
3. Each settlement message carries both `predictedWinnerId` (from the bet) and `actualWinnerId` (from the outcome).
4. The RocketMQ consumer loads the bet from the database, computes `WON` or `LOST` from the persisted predicted winner, and atomically updates `PENDING` to the result.
5. Consumers acknowledge only after processing succeeds. Failures are logged and returned as failure to the broker for retry.

Bet statuses are `PENDING`, `WON`, and `LOST`.

## Requirements and build

- Java 25 (the Gradle toolchain can also provision it)
- Docker with Compose for external brokers (optional)
- PowerShell commands below; use `./gradlew` on macOS/Linux

```powershell
.\gradlew.bat build --no-daemon
```

## Run locally with deterministic in-memory brokers

`betting.broker-mode` defaults to `embedded`, so no broker installation is needed. The HTTP contract and business behavior are the same in both modes.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

The `local` profile enables the H2 console; the application otherwise uses the embedded brokers by default.

Publish an outcome:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/event-outcomes' -Method Post -ContentType 'application/json' -Body '{"eventId":1001,"eventName":"Team A vs Team B","eventWinnerId":11}'
```

Successful response (HTTP `202 Accepted`):

```json
{"topic":"event-outcomes","eventId":1001,"status":"PUBLISHED"}
```

Example settlement message produced on `bet-settlements`:

```json
{"betId":1,"userId":101,"eventId":1001,"eventName":"Team A vs Team B","eventMarketId":"MATCH_WINNER","predictedWinnerId":11,"actualWinnerId":11,"betAmount":25.00}
```

## Initial data and expected statuses

`schema.sql` and `data.sql` seed these bets on startup:

| betId | userId | eventId | eventMarketId | predicted/eventWinnerId | betAmount |
|---:|---:|---:|---|---:|---:|
| 1 | 101 | 1001 | `MATCH_WINNER` | 11 | 25.00 |
| 2 | 102 | 1001 | `MATCH_WINNER` | 12 | 40.00 |
| 3 | 103 | 1002 | `OVER_2_5` | 21 | 15.50 |

After the example request for event `1001` with actual winner `11`:

| betId | status | reason |
|---:|---|---|
| 1 | `WON` | predicted winner `11` equals actual winner `11` |
| 2 | `LOST` | predicted winner `12` differs from actual winner `11` |
| 3 | `PENDING` | belongs to unrelated event `1002` |

An event with no pending bets produces no settlement messages. Bet IDs are globally unique because `BETS.BET_ID` is the primary key.

### Inspect the database

With the `local` profile active, the H2 console is enabled at <http://localhost:8080/h2-console>. Use JDBC URL `jdbc:h2:mem:betting`, user `sa`, and an empty password, then run:

```sql
SELECT BET_ID, USER_ID, EVENT_ID, EVENT_WINNER_ID, BET_AMOUNT, STATUS FROM BETS;
SELECT * FROM PROCESSED_EVENT_OUTCOMES;
```

The database is in memory and is recreated from `schema.sql` and `data.sql` whenever the application starts.

## Run with real Kafka and RocketMQ

Start the local broker stack:

```powershell
docker compose up -d
```

Compose starts a one-shot `rocketmq-init` service that creates and verifies `bet-settlements` and `bet-settlement-consumer-group` with `mqadmin` before the proxy starts. Wait until `docker compose ps` reports the broker and proxy services as healthy and `betting-rocketmq-init` has exited successfully, then start the app with the Docker profile:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=docker,local'
```

Publish the same example outcome and inspect the H2 console for `WON`/`LOST` statuses. Application logs report each successful settlement.

Stop the broker stack when finished:

```powershell
docker compose down
```

For a clean restart, run `docker compose down` and then `docker compose up -d` again. Only use `docker compose down -v` if you explicitly want to discard local broker data.

### Configuration

| Property | Default | Purpose |
|---|---|---|
| `betting.broker-mode` | `embedded` | `embedded` uses in-process adapters; `external` uses Kafka and RocketMQ |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka bootstrap address |
| `betting.kafka.event-outcomes-topic` | `event-outcomes` | Kafka topic name |
| `betting.kafka.consumer-group` | `event-outcome-settlement-group` | Kafka consumer group |
| `betting.kafka.publish-timeout` | `5s` | Maximum wait for Kafka send acknowledgement |
| `betting.rocketmq.endpoints` | `localhost:8081` | RocketMQ 5 gRPC proxy endpoint |
| `betting.rocketmq.bet-settlements-topic` | `bet-settlements` | RocketMQ topic name |
| `betting.rocketmq.consumer-group` | `bet-settlement-consumer-group` | RocketMQ consumer group |
| `betting.rocketmq.enable-ssl` | `false` | TLS for the local Docker proxy |

Kafka is advertised on `localhost:9092`. RocketMQ's proxy maps host ports `8080` and `8081`; the application uses `8081`.

## Validation and errors

- Missing, blank, null, or non-positive mandatory HTTP fields return HTTP `400` with a `ProblemDetail` and field errors.
- Kafka and RocketMQ messages are validated at the consumer boundary; invalid messages are not acknowledged and do not change bet state.
- Malformed JSON and other framework client errors return HTTP `400`, not `500`; unknown paths return `404`.
- Kafka publication failure, timeout, interruption, or serialization failure returns HTTP `500` with a safe `ProblemDetail`. No stack traces, SQL, credentials, or broker internals are returned.
- Successful publication returns HTTP `202` only after Kafka acknowledges the send.

## Duplicates and idempotency

- A repeated event outcome for the same `eventId` is recorded once in `PROCESSED_EVENT_OUTCOMES` and produces no additional settlement messages, even if the repeated outcome has a different name or winner. The primary key makes the insert-if-absent operation atomic; a racing duplicate is treated as a no-op rather than an error.
- Only `PENDING` bets are queried for settlement messages.
- A repeated settlement message for a bet already in the resulting status is a successful no-op.
- Settlement uses an atomic conditional update from `PENDING` to `WON`/`LOST`. Competing updates cannot both transition the same bet.
- A settlement whose requested result conflicts with an already final status fails with a conflict exception; the RocketMQ listener returns failure so the broker can retry. A settlement for a missing bet fails explicitly.
- There is no distributed transaction, outbox, external deduplication store, or locking layer.

## Tests

```powershell
.\gradlew.bat test --no-daemon
```

Covered behavior includes winning and losing bets, multiple bets for one event, unrelated event IDs, events with no bets, duplicate and concurrently raced outcomes, repeated and conflicting settlements, nonexistent bets, concurrent settlement transitions, invalid consumer messages, HTTP validation and publication failures, broker-mode bean selection, an actual Kafka round trip through the external adapter, and final `WON`/`LOST`/`PENDING` database state. Asynchronous checks use Awaitility or Mockito timeouts rather than fixed sleeps.

## Structure

- `controller`: HTTP endpoint and response mapping
- `service`: publication, settlement-message generation, and transactional settlement rules
- `repository`: Spring Data JDBC repositories and conditional status update
- `model`: immutable records and `BetStatus`
- `messaging.kafka`: Kafka publisher and listener
- `messaging.rocketmq`: RocketMQ 5 gRPC publisher, listener, and client configuration
- `messaging.embedded`: deterministic in-process adapters for local use and tests
- `config`: properties and mode-specific wiring
- `exception`: centralized safe `ProblemDetail` handling

## Assumptions and limitations

- H2 is in-memory and single-service; restarting the app resets all bets and processed outcomes.
- The local Docker stack explicitly creates the RocketMQ topic and consumer group through `rocketmq-init` and is intended for demonstration, not production hardening.
- No payout calculation, authentication, authorization, Kubernetes deployment, or cross-broker transaction is included.
- Concurrent settlement correctness is limited to the atomic `PENDING` status transition; it is not a general distributed transaction guarantee.
