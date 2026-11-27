# Sports Betting Settlement Trigger Service

Spring Boot service that publishes sports event outcomes to Kafka, turns pending bets into RocketMQ settlement messages, and persists bet settlement results in an in-memory H2 database.

The primary assignment demonstration is the real Kafka and RocketMQ flow described below. Embedded brokers remain available as a deterministic local fallback.

## Architecture and flow

1. `POST /api/event-outcomes` validates the outcome and publishes it to the Kafka topic `event-outcomes`.
2. The Kafka consumer loads every **PENDING** bet for the outcome's `eventId` and publishes one `bet-settlements` RocketMQ message per bet.
3. Each settlement message carries both `predictedWinnerId` (from the bet) and `actualWinnerId` (from the outcome).
4. The RocketMQ consumer loads the bet from the database, verifies that the message `eventId` matches the persisted bet's `eventId`, computes `WON` or `LOST` from the persisted predicted winner, and atomically updates `PENDING` to the result.
5. Kafka uses `AckMode.RECORD` and an explicit `DefaultErrorHandler` with zero delay and nine retries, for ten total delivery attempts. After recovery, the default recoverer logs the failure and the offset is committed. No dead-letter topic is configured, so an unsuccessful Kafka record can eventually be acknowledged. Invalid internal messages follow the same path because they are raised as runtime exceptions. The RocketMQ listener returns `ConsumeResult.FAILURE` and leaves redelivery to the broker.

Bet statuses are `PENDING`, `WON`, and `LOST`.

## Requirements and build

- Docker with Compose is enough for the primary real-broker demonstration: the app image builds itself with Java 25.
- Java 25 must be installed and selected for local Gradle commands (`build`, `bootRun`, tests). `build.gradle.kts` declares a Java 25 toolchain, but no automatic toolchain provisioning resolver is configured.
- Run the commands from the repository root. Docker Compose commands are shared across Windows and macOS.

**Windows PowerShell**

```powershell
.\gradlew.bat build --no-daemon
```

**macOS Terminal**

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
sh ./gradlew build --no-daemon
```

You do not need Java installed locally for the Docker demonstration.

## Primary demonstration: real Kafka and RocketMQ

Start the whole stack, including the app, from a clean state (this removes only this project's containers):

```
docker compose rm -sf app kafka nameserver broker rocketmq-init proxy
docker compose up -d --build --wait --wait-timeout 300
docker compose ps
```

Compose builds the app image, starts Kafka with an internal listener for containers and an external listener for host tools, starts the RocketMQ nameserver/broker/proxy, and runs one-shot `rocketmq-init` to create `bet-settlements` and `bet-settlement-consumer-group`. `up --wait` returns only when Kafka, the RocketMQ services, and the app are healthy; the init container exits `0`. The app is then available on host port `8080`; the RocketMQ proxy uses host port `8081`; host-side Kafka tools use `localhost:9092`.

Optional checks:

```
docker compose logs -f app
docker compose exec -T nameserver sh mqadmin topicRoute -n localhost:9876 -t bet-settlements
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Publish the example outcome from the host.

**Windows PowerShell**

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/event-outcomes' -Method Post -ContentType 'application/json' -Body '{"eventId":1001,"eventName":"Team A vs Team B","eventWinnerId":11}'
```

**macOS Terminal**

```sh
curl -i --request POST http://localhost:8080/api/event-outcomes \
  --header 'Content-Type: application/json' \
  --data '{"eventId":1001,"eventName":"Team A vs Team B","eventWinnerId":11}'
```

### Verify the result

Follow the app logs:

```
docker compose logs -f app
```

Press `Ctrl+C` to stop following. The expected lines are:

```text
Published 2 settlement(s) for eventId=1001 winnerId=11
Settled betId=1 eventId=1001 status=WON
Settled betId=2 eventId=1001 status=LOST
```

In PowerShell, you can filter older lines with:

```powershell
docker compose logs app | Select-String -Pattern 'Published 2 settlement|Settled betId'
```

Then open the H2 console as described below and confirm bets `1 WON`, `2 LOST`, and `3 PENDING`.

Stop the whole stack when finished:

```
docker compose rm -sf app kafka nameserver broker rocketmq-init proxy
```

Do not use `docker compose down -v` unless you explicitly want to discard local broker data.

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

An event with no pending bets produces no settlement messages. Bet IDs are globally unique because `bet.bet_id` is the primary key.

### Inspect the database

Open the H2 console at <http://localhost:8080/h2-console> in either broker mode. Compose enables H2 console access from the host for this local demo; a locally started app only needs loopback access. Use JDBC URL `jdbc:h2:mem:betting`, user `sa`, and an empty password, then run:

```sql
SELECT BET_ID, USER_ID, EVENT_ID, EVENT_WINNER_ID, BET_AMOUNT, STATUS FROM BET;
SELECT * FROM PROCESSED_EVENT_OUTCOME;
```

The database is in memory and is recreated from `schema.sql` and `data.sql` whenever the application starts.

## Optional deterministic fallback: embedded brokers

`betting.broker-mode` defaults to `embedded`, so no broker installation is needed. The HTTP contract and business behavior are the same in both modes.

**Windows PowerShell**

```powershell
.\gradlew.bat bootRun
```

**macOS Terminal**

```sh
sh ./gradlew bootRun
```

From a second terminal, publish the same outcome using the command for your platform in the [primary demonstration](#primary-demonstration-real-kafka-and-rocketmq).

Successful response (HTTP `202 Accepted`):

```json
{"topic":"event-outcomes","eventId":1001,"status":"PUBLISHED"}
```

Example settlement message produced on `bet-settlements`:

```json
{"betId":1,"userId":101,"eventId":1001,"eventName":"Team A vs Team B","eventMarketId":"MATCH_WINNER","predictedWinnerId":11,"actualWinnerId":11,"betAmount":25.00}
```

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `betting.broker-mode` | `embedded` | `embedded` uses in-process adapters; `external` uses Kafka and RocketMQ |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka bootstrap address |
| `betting.kafka.event-outcomes-topic` | `event-outcomes` | Kafka topic name |
| `betting.kafka.consumer-group` | `event-outcome-settlement-group` | Kafka consumer group |
| `betting.kafka.publish-timeout` | `5s` | Shared time budget for Kafka producer blocking and acknowledgement waiting. Also sets the producer's `max.block.ms` in external mode |
| `betting.rocketmq.endpoints` | `localhost:8081` | RocketMQ 5 gRPC proxy endpoint |
| `betting.rocketmq.bet-settlements-topic` | `bet-settlements` | RocketMQ topic name |
| `betting.rocketmq.consumer-group` | `bet-settlement-consumer-group` | RocketMQ consumer group |
| `betting.rocketmq.enable-ssl` | `false` | TLS for the local Docker proxy |

Kafka is advertised on `localhost:9092`. RocketMQ's proxy maps only host port `8081`; host port `8080` is intentionally left free for the Spring Boot application.

## Validation and errors

- Missing, blank, null, or non-positive mandatory HTTP fields return HTTP `400` with a `ProblemDetail` and field errors.
- Unsupported HTTP methods return `405`, unsupported media types return `415`, and unknown paths return `404`, each with a safe `ProblemDetail`.
- Kafka and RocketMQ messages are validated at the consumer boundary; invalid messages never change bet state. Kafka invalid records are retried by the container error handler and then logged/skipped after recovery; RocketMQ returns `ConsumeResult.FAILURE` for broker redelivery.
- A settlement whose `eventId` does not match the persisted bet is rejected before any status update.
- Malformed JSON returns HTTP `400`. Kafka publication failure, timeout, interruption, or serialization failure returns HTTP `500` with a safe `ProblemDetail`. No stack traces, SQL, credentials, or broker internals are returned.
- Successful publication returns HTTP `202` only after Kafka acknowledges the send.
- If the acknowledgement misses the HTTP deadline, Kafka may still deliver a record it has already accepted for sending.

## Duplicates and idempotency

- A repeated event outcome for the same `eventId` is recorded once in `processed_event_outcome` and produces no additional settlement messages. The primary key makes the insert-if-absent operation atomic; a racing duplicate is treated as a no-op rather than an error.
- An identical repeated winner is logged as a duplicate. A repeated outcome with a different winner is explicitly logged as a conflicting outcome, but the first outcome still wins and bet state is unchanged.
- Only `PENDING` bets are queried for settlement messages.
- A repeated settlement message for a bet already in the resulting status is a successful no-op.
- Settlement uses an atomic conditional update from `PENDING` to `WON`/`LOST`. Competing updates cannot both transition the same bet.
- A settlement whose requested result conflicts with an already final status fails with a conflict exception; the RocketMQ listener returns failure so the broker can retry. A settlement for a missing bet fails explicitly.
- If RocketMQ publication fails partway through an event, the Kafka listener throws and its transaction rolls back the processed-outcome registration. The retried event republishes the settlements; already-delivered settlement messages are harmless because the status transition is idempotent.
- There is no distributed transaction, outbox, external deduplication store, or locking layer.

## Tests

**Windows PowerShell**

```powershell
.\gradlew.bat test --no-daemon
```

**macOS Terminal**

```sh
sh ./gradlew test --no-daemon
```

Covered behavior includes winning and losing bets, multiple bets for one event, unrelated event IDs, events with no bets, identical and conflicting duplicate outcomes, concurrently raced outcomes, repeated and conflicting settlements, mismatched settlement event IDs, nonexistent bets, concurrent settlement transitions, invalid consumer messages, HTTP validation and `405`/`415` status preservation, partial RocketMQ publication failure followed by event retry, broker-mode bean selection, an actual Kafka round trip through the external adapter, and final `WON`/`LOST`/`PENDING` database state. Test-only embedded broker subscriptions are released after each test to prevent subscriber accumulation. Asynchronous checks use Awaitility or Mockito timeouts rather than fixed sleeps.

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
