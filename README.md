# Jdentifiers

[![Maven Central](https://img.shields.io/maven-central/v/dk.ceti.jdentifiers/jdentifiers-id)](https://central.sonatype.com/artifact/dk.ceti.jdentifiers/jdentifiers-id)
[![javadoc](https://javadoc.io/badge2/dk.ceti.jdentifiers/jdentifiers-id/javadoc.svg)](https://javadoc.io/doc/dk.ceti.jdentifiers/jdentifiers-id)
[![License](https://img.shields.io/github/license/tnn/jdentifiers)](LICENSE)

Type-safe, k-sortable identifier library for Java.

Phantom-typed wrappers around numeric values (`long`, `int`, `UUID`) that prevent mixing identifiers for different
domain entities at compile time. Two generation strategies, random and k-sortable (time-sorted), are hidden behind a
common `IDGenerator` interface, so consuming code is unaware of how an identifier was generated.

```java
ID<Organization> orgId = generator.identifier();
ID<User> userId = generator.identifier();

// Compile error: incompatible types
service.getUser(orgId);
```

## Identifier types

| Type     | Storage        | Hex string  | Use case                                                                                                                                                                   |
|----------|----------------|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `LID<T>` | 32-bit `int`   | 8 chars     | Locally scoped within a composite key, e.g. `(organization_id, team_id)`. Not globally unique.                                                                             |
| `ID<T>`  | 64-bit `long`  | 16 chars    | Entity primary key within a single system. Sufficient for the vast majority of workloads — Twitter's ~6,000 tweets/sec would take ~200 million years to exhaust the space. |
| `GID<T>` | 128-bit `UUID` | UUID format | Globally unique across systems. Conforms to UUID v4 (random) or v7 (k-sortable).                                                                                           |

All three are immutable, `Serializable`, `Comparable`, and carry a phantom type parameter `<T extends IDAble>` for
compile-time entity discrimination.

## Generation strategies

```
IDGenerator (interface)
├── RandomIDGenerator      — SecureRandom bits, no ordering guarantees
└── KSortableIDGenerator   — time-sorted, monotonic within a clock tick
```

The types are opaque wrappers; a consumer holding an `ID<User>` cannot tell how it was generated.

```java
// Random
IDGenerator random = new RandomIDGenerator();

// K-sortable with auto-detected node ID (MAC, K8s, hostname, random)
IDGenerator gen = new KSortableIDGenerator();

// K-sortable with explicit node ID
IDGenerator gen = KSortableIDGenerator.builder()
        .nodeId(NodeIdStrategies.of(10, 42))
        .build();

// Kubernetes-aware (decodes Deployment pod suffix, SHA-256 fallback)
IDGenerator gen = KSortableIDGenerator.builder()
        .nodeId(NodeIdStrategies.kubernetes(10))
        .build();
```

The default constructor and `builder().build()` both use `NodeIdStrategies.auto(10)`, which
tries MAC address, K8s Deployment hostname, hostname SHA-256, and random, in that order.

## Modules

| Module                    | Contents                                                                                                           |
|---------------------------|--------------------------------------------------------------------------------------------------------------------|
| **id**                    | Core types (`ID`, `LID`, `GID`), `IDGenerator`, `RandomIDGenerator`, `KSortableIDGenerator`, `NodeIdStrategies`    |
| **jackson**               | Jackson `JsonSerializer` / `JsonDeserializer` for all three types                                                  |
| **kotlinx-serialization** | kotlinx-serialization `KSerializer` implementations                                                                |
| **micronaut**             | Micronaut `TypeConverter` bindings                                                                                 |
| **benchmarks**            | JMH latency benchmarks                                                                                             |

## Build & test

```sh
mvn test                    # all modules
mvn -pl id test             # core module only
```

## Benchmarks (JMH)

```sh
mvn -pl id,benchmarks package -DskipTests
mvn -pl benchmarks exec:exec -Dbenchmark=IDGenerationBenchmark
mvn -pl benchmarks exec:exec -Dbenchmark=IDStringBenchmark
```

## Encoding and serialization

`toString()` and `fromString()` on all three types use lowercase hex.
`fromString()` accepts mixed-case input. The library has a single strategy on purpose,
although it does not prevent other formats such as Base32 or Base64.

**Why hex?**

| Encoding | `LID` (32-bit) | `ID` (64-bit) | `GID` (128-bit) | Overhead  | Sort    | Notes                                                             |
|----------|----------------|---------------|-----------------|-----------|---------|-------------------------------------------------------------------|
| Binary   | 4 bytes        | 8 bytes       | 16 bytes        | —         | Yes     | Best for storage, not usable in text protocols                    |
| Base64   | 6 chars        | 11 chars      | 22 chars        | +38%      | No      | Less legible (`I`/`l`, `0`/`O`), may break double-click selection |
| Base32   | 7 chars        | 13 chars      | 26 chars        | +63%      | No      | Less legible depending on alphabet (`I`/`1`)                      |
| **Hex**  | **8 chars**    | **16 chars**  | **32 chars**    | **+100%** | **Yes** | **Legible, universally parseable, double-click selectable**       |
| Decimal  | 10 chars       | 20 chars      | 39 chars        | +150%     | Yes     | Longest; 64-bit values exceed JS `Number.MAX_SAFE_INTEGER`        |
| UUID     | —              | —             | 36 chars        | +125%     | Yes     | Standard format for 128-bit; used by `GID.toString()`             |

Hex trades compactness for safety:

- Sort order is preserved: Big-endian hex means fx `strcmp` gives the same result as unsigned
  numeric comparison, so `ORDER BY id_hex` in a database text column sorts chronologically
  (see [ADR-006](docs/adr/adr-006-big-endian-encoding.md)).
- No precision loss: Decimal representations of 64-bit IDs exceed 2^53, which means any
  JSON consumer backed by IEEE 754 doubles (JavaScript, Python's `json` module) will silently
  round the value (this problem forced Twitter to ship `id_str` alongside `id`).
  Hex strings round-trip through every JSON parser without loss.
- Legibility: Hex characters are easy to read aloud and can be selected with a double-click in
  terminals and browsers. Base64 and Base32 use characters that look alike in many fonts and are
  harder to communicate verbally. The tradeoff is a longer string.

Why one codec, not pluggable: The `toString()`/`fromString()` contract is used by the
serialization modules, and by any code that stores IDs as strings. If different parts of a 
system disagree on the encoding, IDs become unparseable at the boundary. 
A single default eliminates that failure mode. If you need Base64 for `ID`, it is available via 
`toBase64String()`/`fromBase64String()`, but it is opt-in and not wired into the serialization modules.

If there is push for it, we can add base32 and/or base64 modules as well. Hex is meant as a conservative default.
---

# Architecture

## K-sortable bit layouts

### GID (128-bit) — UUIDv7, RFC 9562

```
MSB [63 ·························································· 0]
     ┌──────────────────────────────────────┬────┬──────────────┐
     │    48-bit Unix ms timestamp          │ver │  12-bit      │
     │    (ms since 1970-01-01)             │0111│  rand_a      │
     └──────────────────────────────────────┴────┴──────────────┘
LSB [63 ·························································· 0]
     ┌──┬──────────────────────────────────────────────────────────┐
     │10│                    62-bit rand_b                         │
     └──┴──────────────────────────────────────────────────────────┘
```

- **Epoch**: Unix 1970 (RFC 9562).
- **Monotonicity**: `rand_a` is a 12-bit counter that resets to a small random offset ([0, 256)) each new millisecond
  (RFC 9562 Method 1), giving ~3,840 guaranteed sequential IDs per ms per generator.
- **Overflow**: blocks until the next ms tick.
- **Sort correctness**: `GID.compareTo` uses `Long.compareUnsigned` on both halves.

### ID (64-bit) — TSID-style, unsigned

```
[63 ·························································· 0]
 ┌─────────────────────────────────────────┬───────────────────┐
 │  42-bit ms timestamp                    │  22-bit payload   │
 │  (ms since 2020-01-01)                  │  [node | counter] │
 └─────────────────────────────────────────┴───────────────────┘
```

- **Epoch**: 2020-01-01T00:00:00Z. See [ADR-005](docs/adr/adr-005-custom-epoch.md).
- **Timestamp range**: 2^42 ms ≈ 139 years. Rolls over 2159-05-15.
- **Payload**: the 22-bit payload is split between a node ID and a monotonic counter, configured
  at construction time via `NodeIdSupplier`:

| Configuration    | Node bits | Counter bits | Max nodes | IDs/ms/node |
|------------------|-----------|--------------|-----------|-------------|
| Single-node      | 0         | 22           | 1         | 4,194,304   |
| Small cluster    | 5         | 17           | 32        | 131,072     |
| Medium cluster   | 10        | 12           | 1,024     | 4,096       |
| Large cluster    | 12        | 10           | 4,096     | 1,024       |

The default (`auto(10)`) uses 10 node bits and 12 counter bits.

- **Overflow**: blocks until the next ms tick.

### LID (32-bit) — hour-precision, scoped

```
[31 ···························· 0]
 ┌──────────────────┬─────────────┐
 │  20-bit hour     │  12-bit     │
 │  timestamp       │  counter    │
 └──────────────────┴─────────────┘
```

- **Epoch**: configurable (default 2020-01-01T00:00:00Z).
- **Precision**: 1 hour. Clock regression within the same hour bucket is harmless.
- **Timestamp range**: 2^20 hours ≈ 119 years. Rolls over ~2139 from default epoch.
- **Counter**: 4,096 values per hour per scope.
- **No node component**: LIDs are scoped within composite keys, so node entropy is unnecessary.
- **Overflow**: wraps to zero by default (`LidOverflowPolicy.WRAP`). The previous fail-fast
  behaviour is available via `LidOverflowPolicy.THROW`. See [ADR-014](docs/adr/adr-014-lid-overflow-wrap-default.md).

### When NOT to use k-sortable LID

K-sortable LID is appropriate only when:

1. The LID is scoped within a composite key, so collisions are per-scope.
2. The generation rate per scope is low (tens to hundreds per hour).
3. Hour-precision ordering is sufficient.

For higher throughput or globally unique 32-bit values, use `RandomIDGenerator.localIdentifier()`.

---

## Architectural Decision Records

Full ADRs are in [`docs/adr/`](docs/adr/adr-000-index.md).

| ADR                                                            | Decision                                                         |
|----------------------------------------------------------------|------------------------------------------------------------------|
| [001](docs/adr/adr-001-unsigned-comparison.md)                 | Unsigned comparison everywhere                                   |
| [002](docs/adr/adr-002-phantom-type-parameter.md)              | Phantom type `<T extends IDAble>` for compile-time safety        |
| [003](docs/adr/adr-003-comparable-wildcard.md)                 | `Comparable<XID<?>>` wildcard for mixed-type sorting             |
| [004](docs/adr/adr-004-opaque-types.md)                        | Opaque types, bit-layout logic in the generator                  |
| [005](docs/adr/adr-005-custom-epoch.md)                        | Custom epoch 2020-01-01                                          |
| [006](docs/adr/adr-006-big-endian-encoding.md)                 | Big-endian encoding, hex sort = numeric sort                     |
| [007](docs/adr/adr-007-serializable-pinned-uid.md)             | `Serializable` with pinned `serialVersionUID`                    |
| [008](docs/adr/adr-008-securerandom-sha1prng.md)               | SHA1PRNG / DRBG for non-blocking latency                         |
| [009](docs/adr/adr-009-clock-regression-policy.md)             | Clock regression: spin ≤1s, throw >1s                            |
| [010](docs/adr/adr-010-counter-overflow-policy.md)             | Counter overflow: block (ID/GID) vs throw (LID)                  |
| [011](docs/adr/adr-011-no-common-base-interface.md)            | No common base interface for ID types                            |
| [012](docs/adr/adr-012-synchronized-thread-safety.md)          | Thread safety via `synchronized`                                 |
| [013](docs/adr/adr-013-shared-counter-across-phantom-types.md) | Shared counter across phantom types                              |
| [014](docs/adr/adr-014-lid-overflow-wrap-default.md)           | LID overflow wraps by default                                    |
| [015](docs/adr/adr-015-node-id-supplier.md)                    | `NodeIdSupplier` replaces nodeBits/nodeId/nodeIdFactory          |

---

## Operational limits

| Property                      | ID (64-bit)            | LID (32-bit)                      | GID (128-bit)            |
|-------------------------------|------------------------|-----------------------------------|--------------------------|
| Timestamp bits                | 42                     | 20                                | 48                       |
| Timestamp precision           | 1 ms                   | 1 hour                            | 1 ms                     |
| Timestamp range               | ~139 years             | ~119 years                        | ~8,919 years             |
| Rollover date (default epoch) | 2159-05-15             | ~2139                             | ~10889                   |
| Counter bits (default)        | 12 (with 10 node bits) | 12                                | 12                       |
| Max IDs per tick per node     | 4,096 (default)        | 4,096                             | ~3,840 (randomized init) |
| Counter overflow              | Block ≤1ms             | Wrap (default) or throw           | Block ≤1ms               |
| Clock regression tolerance    | ≤1s spin, >1s throw    | Hour-boundary throw               | ≤1s spin, >1s throw      |
| Epoch                         | 2020-01-01             | Configurable (default 2020-01-01) | Unix 1970 (RFC 9562)     |

## Prior art

| Scheme              | Bits    | Timestamp | Precision | Node                    | Counter/Random       | Epoch           |
|---------------------|---------|-----------|-----------|-------------------------|----------------------|-----------------|
| **Jdentifiers ID**  | **64**  | **42**    | **1 ms**  | **0-22 (configurable)** | **22-0 monotonic**   | **Custom 2020** |
| Snowflake (Twitter) | 64      | 41        | 1 ms      | 10 (fixed)              | 12 sequential        | Custom          |
| TSID (Hibernate)    | 64      | 42        | 1 ms      | 0-22 (configurable)     | 22-0 sequential      | Custom 2020     |
| **Jdentifiers GID** | **128** | **48**    | **1 ms**  | **none**                | **12 seq + 62 rand** | **Unix 1970**   |
| UUIDv7 (RFC 9562)   | 128     | 48        | 1 ms      | none                    | 12 seq + 62 rand     | Unix 1970       |
| ULID                | 128     | 48        | 1 ms      | none                    | 80 random            | Unix 1970       |
| KSUID (Segment)     | 160     | 32        | 1 s       | none                    | 128 random           | Custom 2014     |

Jdentifiers ID is closest to [TSID](https://github.com/vladmihalcea/hypersistence-tsid).
The difference is that the `ID<T>` wrapper is opaque and generator-agnostic.

## Java version

Requires JDK 17+ (build requires JDK 21+).

## Credits & related work

- Snowflake (Twitter), https://github.com/twitter-archive/snowflake, https://dl.acm.org/doi/10.5555/70413.70419
- TSID (Vlad Mihalcea /
  Hibernate), https://vladmihalcea.com/tsid-identifier-jpa-hibernate/, https://github.com/vladmihalcea/hypersistence-tsid
- KSUID (Segment), https://github.com/segmentio/ksuid, https://github.com/ksuid/ksuid
- ULID, https://github.com/ulid, https://github.com/azam/ulidj
- TypeID, https://github.com/jetpack-io/typeid
- UUIDv7 (RFC 9562), https://www.rfc-editor.org/rfc/rfc9562
