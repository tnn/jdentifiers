# ADR-013: Shared counter across phantom types

**Status**: Accepted

## Context

`KSortableIDGenerator` maintains a single counter per ID width (64-bit, 128-bit, 32-bit). All phantom-typed IDs of the same width — `ID<User>`, `ID<Organization>`, etc. — draw from the same counter and contend on the same lock. Since phantom types represent separate logical namespaces that are never compared or stored together, the question is whether partitioning counters per entity type would improve throughput without meaningful cost.

## Decision

Keep the shared counter. Do not partition by phantom type.

### Cross-type numeric uniqueness survives type erasure

With a shared counter, two IDs generated in the same millisecond from the same generator always have different `long` values, regardless of phantom type. This is a critical safety property because:

- `ID.equals()` compares the underlying value only — the phantom type `T` is erased at runtime (ADR-002). Two IDs of different entity types with the same numeric value are `.equals() == true`.
- Untyped storage (audit logs, generic caches, `Map<Long, Object>`, JSON/logging) silently conflates IDs that share a numeric value.
- For a library used by millions, many users will store IDs as bare `long` values. Implicit cross-type uniqueness is a safety net they never have to think about.

Per-type counters would break this: `ID<User>` and `ID<Organization>` could produce identical `long` values for the same timestamp + counter position.

### Throughput is not a real bottleneck

Even the smallest counter configuration (12-bit, 4,095/ms with 10 node bits) supports ~4.1 million IDs/sec. The `synchronized` critical section (~60–90 ns) limits a single lock to ~11,000–16,000 calls/ms, which is reached before the counter overflows. Per-type partitioning would multiply a ceiling that is already unreachable from a single JVM.

### Lock contention is marginal at realistic thread counts

At 8 threads, worst-case lock wait is ~560 ns. At 32 threads, ~2.5 µs. ID generation is a tiny fraction of request processing; the lock disappears in the noise for realistic service architectures.

## Escape hatch

Users who genuinely need per-entity-type counter isolation (independent throughput ceilings, zero cross-type lock contention) can create separate generator instances:

```java
KSortableIDGenerator.Builder base = KSortableIDGenerator.builder()
    .nodeBits(10).nodeId(42);
KSortableIDGenerator userGen = base.copy().build();
KSortableIDGenerator orgGen  = base.copy().build();
```

Each instance has its own counters, locks, and `SecureRandom`. Cost: ~400 bytes per instance. Trade-off: cross-type numeric uniqueness is lost — use only when entity types are stored in type-discriminated columns or tables.

## Alternatives rejected

| Alternative | Why rejected |
|---|---|
| `identifier(Class<T>)` overload | Pollutes `IDGenerator` interface; cannot be added without breaking existing implementations; ceremony for the 99% case that doesn't need it. |
| Internal `ClassValue<CounterState>` | Requires `Class<T>` at runtime, which type erasure prevents without an API change. |
| `TypedIDGeneratorFactory` wrapper | Solves a problem that doesn't exist at realistic workloads; primary effect is enabling cross-type numeric collisions behind an abstraction. |

## Consequence

The shared counter is the conservative default: maximum safety, sufficient performance. Users who need isolation opt in explicitly via separate instances, accepting the trade-off with full awareness.
