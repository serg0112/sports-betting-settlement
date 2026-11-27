# Sports Betting Settlement Trigger Service

Spring Boot backend that simulates sports betting event outcome handling and bet settlement through Kafka and RocketMQ.

## Flow

1. `POST /api/event-outcomes` validates an event outcome and publishes it to the `event-outcomes` Kafka topic.
2. The Kafka consumer matches the outcome with in-memory bets by `eventId` and winner.
3. Matching bets are published to the `bet-settlements` RocketMQ topic.
4. The RocketMQ consumer receives each command and settles/logs the bet.

The HTTP contract is the same in both run modes.

## Requirements

- Java 25 (the Gradle toolchain can also download it)
- Docker with Compose for external brokers (optional)
- Windows PowerShell commands below; use `./gradlew` on macOS/Linux

## Run without Docker (default)

The default `betting.broker-mode=embedded` uses deterministic in-process broker adapters, so the full flow can be exercised immediately:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\openjdk-25.0.1"; .\gradlew.bat bootRun
```

From another terminal:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/event-outcomes' -Method Post -ContentType 'application/json' -Body '{"eventId":1001,"eventName":"Team A vs Team B","eventWinnerId":11}'
# topic       status
# event-outcomes PUBLISHED
```

Initial bets:

| betId | userId | eventId | eventWinnerId | betAmount |
|---:|---:|---:|---:|---:|
| 1 | 101 | 1001 | 11 | 25.00 |
| 2 | 102 | 1001 | 12 | 40.00 |
| 3 | 103 | 1002 | 21 | 15.50 |

The example settles bet 1 only. An unknown event or unmatched winner produces no settlement.

## Run with real Kafka and RocketMQ

Start the brokers:

```powershell
docker compose up -d
```

Wait until the containers are healthy, then start the app in external mode:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\openjdk-25.0.1"; .\gradlew.bat bootRun --args='--spring.profiles.active=docker'
```

Publish an outcome:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/event-outcomes' -Method Post -ContentType 'application/json' -Body '{"eventId":1001,"eventName":"Team A vs Team B","eventWinnerId":11}'
```

Inspect settlement messages with the RocketMQ admin tool:

```powershell
docker exec -it betting-rocketmq-broker sh mqadmin consumeMessage -n nameserver:9876 -t bet-settlements -g bet-settlement-consumer-group
```

Stop and remove the broker containers when finished:

```powershell
docker compose down
```

### Configuration

| Property | Default | Purpose |
|---|---|---|
| `betting.broker-mode` | `embedded` | `embedded` uses in-process brokers; `external` uses Kafka and RocketMQ |
| `betting.kafka.bootstrap-servers` | `localhost:9092` | Kafka bootstrap address |
| `betting.kafka.event-outcomes-topic` | `event-outcomes` | Kafka topic name |
| `betting.kafka.consumer-group` | `event-outcome-settlement-group` | Kafka consumer group |
| `betting.rocketmq.endpoints` | `localhost:8081` | RocketMQ gRPC proxy endpoint |
| `betting.rocketmq.bet-settlements-topic` | `bet-settlements` | RocketMQ topic name |
| `betting.rocketmq.consumer-group` | `bet-settlement-consumer-group` | RocketMQ consumer group |
| `betting.rocketmq.enable-ssl` | `false` | TLS for the local Docker proxy |

## Tests

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\openjdk-25.0.1"; .\gradlew.bat test
```

Tests cover the business rules, in-memory bets, the full HTTP-to-settlement flow, validation errors, repeated calls, and a real Kafka topic produced/consumed through an embedded Kafka broker.

## Structure

- `controller`: HTTP endpoint and request/response handling
- `service`: publish, match, and settle business rules
- `repository`: in-memory bet store
- `model`: immutable records used across layers
- `messaging.kafka`: Kafka producer and consumer
- `messaging.rocketmq`: Apache RocketMQ 5 gRPC producer and consumer
- `messaging.embedded`: deterministic in-process brokers used by default/tests
- `config`: topic names, broker-mode selection, and external settings
- `exception`: centralized `ProblemDetail` error handling
