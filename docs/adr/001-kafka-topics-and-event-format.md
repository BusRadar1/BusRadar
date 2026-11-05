# ADR-001: Kafka topics and event format

* **Status:** Proposed
* **Date:** 2026-11-05
* **Author:** jakub-jurkian (streaming)
* **Deciders:** team (to be accepted at the Wednesday meeting)

## Context

ingest-service reads vehicle positions from the ZTM API, and three consumers use the same readings independently: tracking (latest position in Redis, live map), the stop passage detector, and stats-service (raw history in PostgreSQL). Those consumers are written by different people, in parallel, in the same weeks. Without a fixed contract none of them can start, because the producer and every consumer have to agree on the topic name, the message key, the payload fields and their units before that code exists.

Constraints that drive the decision:

* One Kafka broker in `infra/docker-compose.yml`, no schema registry, and topic auto-creation is disabled (`KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"`), so topics must be declared by the application.
* The ZTM API (`https://ckan2.multimediagdansk.pl/gpsPositions?v=2`) returns a full snapshot of roughly 400 vehicles about every 20 s. Each vehicle carries its own `generated` timestamp, and `tripId` and `scheduledTripStartTime` are null for a vehicle without an assigned trip.
* Statistics come from stop passages, not from raw readings, so raw readings only have to survive long enough to be consumed and stored. The server collects 24/7 from 23.11.2026, so a consumer that is down has to catch up without losing readings.
* The API sometimes reports values that are not usable as they stand: a `delay` of 3827 s and `gpsQuality` of 0 both appear in a single one-minute sample. Someone has to own the decision of who discards them.

This ADR fixes the contract. The choice of Kafka over another broker is a separate decision (ADR-002).

## Decision

One topic carries every raw reading, keyed by vehicle, with a JSON payload defined by a Java record shared through `backend/libs/events`. Each consumer reads that topic in its own consumer group, so all of them see every reading and none blocks another. ingest-service publishes what the API returned, without filtering, and consumers decide what to ignore. The sections below are the contract; a change to any of them needs a new ADR.

### 1. Topics

| Topic | Carries | Payload defined |
| --- | --- | --- |
| `vehicle-positions` | every reading from the ZTM API | here, section 6 |
| `vehicle-positions.dlt` | readings that could not be parsed | here, section 7 |
| `stop-passages` | output of the stop passage detector | in ADR-003 |

`stop-passages` is named here so consumers can be configured against a stable name, but its fields depend on detection rules that are not designed yet. Its name, key, partition count and retention are fixed below; its payload is left to ADR-003.

### 2. Partitions and replication

* **3 partitions** for both topics. With a single broker this is not about throughput; it sets the maximum number of consumers that can share the work inside one group later.
* **Replication factor 1**, because compose runs one broker. Losing that broker's disk loses whatever has not been consumed yet. Accepted for a semester project; the recorder files (#32) are the backup.
* Partition count is effectively frozen. Raising it rehashes keys and breaks the per-vehicle ordering that section 3 relies on.

### 3. Message key

The key is `vehicleId`, serialized as a string, on both topics.

All readings for one vehicle therefore land in the same partition and reach a consumer in the order they were published. That is what makes the rule "an older reading must not overwrite a newer one" enforceable in tracking. Around 400 distinct vehicles also spread evenly over 3 partitions, so no partition becomes a hot spot.

### 4. Retention

| Topic | Retention | Reason |
| --- | --- | --- |
| `vehicle-positions` | 7 days (`retention.ms=604800000`) | About 1–1.4 GB with `compression.type=lz4` on the producer, and long enough for a consumer to be down over a weekend and still catch up. Volume estimate in #34. |
| `vehicle-positions.dlt` | 14 days | Tiny volume, and we want time to notice a problem before the evidence expires. |
| `stop-passages` | 30 days | Few events, and they feed every statistic; a longer window is cheap insurance. |

A consumer down for longer than the retention window loses readings. Recovering those means replaying recorder files, not Kafka.

### 5. Serialization

Payloads are JSON produced by Jackson from the records in `backend/libs/events`.

There is no schema registry in compose, `kafka-console-consumer` shows readable output, and the whole team already works with JSON. The cost is that nothing enforces the schema at write time and payloads are larger than a binary format. Two rules follow from that choice:

* Timestamps are ISO-8601 UTC strings mapped to `Instant`, never epoch milliseconds.
* Consumers must ignore unknown fields (`FAIL_ON_UNKNOWN_PROPERTIES=false`). This is what allows a new optional field to be added without breaking a running consumer.

Kafka's own record timestamp stays at the broker default (producer wall clock). The reading time lives in the payload as `readAt`, because that is the only value that is correct after a replay.

### 6. Event payload: `VehiclePositionEvent`

| Field | Type | Nullable | Source field | Meaning and unit |
| --- | --- | --- | --- | --- |
| `vehicleId` | `long` | no | `vehicleId` | stable vehicle identifier; also the message key, as a string |
| `vehicleCode` | `String` | no | `vehicleCode` | fleet number shown to passengers, e.g. `9107` |
| `vehicleService` | `String` | no | `vehicleService` | service block, e.g. `149-11` |
| `line` | `String` | no | `routeShortName` | line as shown to passengers: `149`, `M32`, `T8` |
| `routeId` | `int` | no | `routeId` | internal route id; not always the number in `line` |
| `tripId` | `Integer` | **yes** | `tripId` | null when the vehicle has no assigned trip; not unique on its own |
| `scheduledTripStart` | `Instant` | **yes** | `scheduledTripStartTime` | null whenever `tripId` is null |
| `headsign` | `String` | no | `headsign` | destination text; may be empty, may contain double spaces |
| `lat` | `double` | no | `lat` | WGS84 degrees |
| `lon` | `double` | no | `lon` | WGS84 degrees |
| `speed` | `int` | no | `speed` | km/h (assumed from observed values; to be confirmed against the dataset description) |
| `direction` | `int` | no | `direction` | degrees, observed only in 45° steps |
| `delaySeconds` | `int` | no | `delay` | seconds against the timetable; negative means early |
| `gpsQuality` | `int` | no | `gpsQuality` | 0 means no usable GPS fix |
| `readAt` | `Instant` | no | `generated` | when the reading was produced, per vehicle, UTC |
| `ingestedAt` | `Instant` | no | set by ingest | when we fetched it, UTC |

A trip is identified by `routeId` + `tripId` + `scheduledTripStart`. `tripId` alone repeats across lines.

This table is the contract as accepted. `VehiclePositionEvent` in `backend/libs/events` is the living source of truth, so an optional field added later (section 7) does not require amending this ADR.

### 7. Rules that follow from this contract

* **Dedup key is (`vehicleId`, `readAt`).** ingest-service does not deduplicate in round 1. stats-service enforces uniqueness with a database constraint.
* **ingest-service does not filter.** `gpsQuality = 0` and extreme `delay` values are published exactly as received. Each consumer decides what to ignore, and the rule it applies belongs in that consumer's own ADR or issue. This keeps the raw history complete and reprocessable.
* **Delivery is at-least-once.** The producer uses `acks=all` with `enable.idempotence=true`; consumers must tolerate duplicates. This is why stats-service inserts with `ON CONFLICT DO NOTHING`.
* **One consumer group per consumer**, so each sees every reading: `tracking`, `stats`, `passage-detector`, all with `auto.offset.reset=earliest` so a new group starts from the oldest retained reading rather than skipping history.
* **The dead-letter topic is for messages that cannot be parsed**: missing `vehicleId`, an unparseable timestamp, malformed JSON. It is not for business-rule violations such as an implausible delay, which are valid messages with odd content.
* **Schema evolution:** adding an optional field needs no new ADR. Removing or renaming a field, or changing a unit, does, together with a coordinated consumer update.

## Considered Options

### Option A: one raw topic, keyed by `vehicleId`, JSON payload, one consumer group per consumer (chosen)

* **Pros:** one fetch from the API feeds every consumer; each group keeps its own offset, so a slow or dead consumer never blocks the others and catches up on restart; replaying a recorded day needs no extra machinery; per-vehicle ordering comes free from the key; a new consumer is added without touching ingest-service.
* **Cons:** every consumer deserializes fields it does not need; a payload change affects all consumers at once; nothing enforces the schema at write time.

### Option B: one topic per consumer, written by ingest-service

Rejected. ingest-service would publish every reading three times and would have to know who its consumers are, so adding a fourth consumer means changing and redeploying it. A payload tailored per consumer does not justify that coupling.

### Option C: one raw topic with Avro and a Schema Registry

Rejected. Enforcing the schema at write time is real value, but it costs another service in compose, generated classes in the build, and readable `kafka-console-consumer` output while we debug the first pipeline. The benefit arrives at a scale this project will not reach.

## Consequences

**Positive**

* ingest-service, stats-service and tracking-service can be built in parallel against a frozen contract, which makes the walking skeleton feasible before 17.11.
* Per-vehicle ordering makes the "never overwrite a newer reading" rule enforceable.
* A recorded day replays through the same topic with no code changes.
* One malformed record goes to the dead-letter topic instead of stopping a consumer.

**Negative**

* The key and the partition count are effectively permanent; changing either breaks ordering for data already published.
* JSON surfaces a renamed or mistyped field at runtime, not at compile time.
* Replication factor 1 loses unconsumed readings if the broker disk fails, and past 7 days only the recorder files still hold them.
* Validation cost moves to every consumer, and stats-service carries most of it.

**Follow-up:** #32 (recorder, source of the fixture for the mapping test), #33 (ZTM client and mapping), #34 (producer and topics), #35 (stats-service consumer and raw table), ADR-003 (`stop-passages` payload), ADR-004 (database schema, partitioning, retention).

## References

* Issue [#31](https://github.com/BusRadar1/BusRadar/issues/31): this ADR and the event record
* [system-flow.md](../system-flow.md): how the reading branches to three consumers
* [how-it-works.md](../how-it-works.md): API endpoints, example readings and volume estimates
* [repo-conventions.md](../repo-conventions.md): planned ADRs and repository layout
* `infra/docker-compose.yml`: broker configuration, listeners and disabled topic auto-creation
* Otwarte dane ZTM w Gdańsku (CC BY): <https://ckan.multimediagdansk.pl/dataset/tristar>
