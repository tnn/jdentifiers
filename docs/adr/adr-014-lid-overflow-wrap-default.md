# ADR-014: LID counter overflow — wrap by default

**Status**: Accepted  
**Supersedes**: ADR-010 (LID section only; ID/GID overflow policy unchanged)

## Context

ADR-010 established that LID counter overflow throws `IllegalStateException`. The rationale was sound at the time: exhausting 4,096 values in one hour was assumed to indicate misuse.

In practice, LIDs are used inside composite primary keys like `(tenant_id, team_lid)` or `(org_id, project_lid)`, where uniqueness is already enforced on the composite. Throwing from the generator is too eager: it refuses to produce a value that the application may never persist, or that the DB would reject with a constraint violation anyway. And once it throws, the service can't generate LIDs again until the hour rolls over or the process restarts — a sharp failure mode for something the caller usually doesn't need protection against.

## Decision

The default LID overflow policy is now `WRAP`: when the 12-bit counter reaches 4,095, the next call resets it to zero. The counter continues incrementing from there.

Users who want the previous fail-fast behavior can opt in:

```java
KSortableIDGenerator gen = KSortableIDGenerator.builder()
    .lidOverflowPolicy(LidOverflowPolicy.THROW)
    .build();
```

The `THROW` policy is unchanged — it throws `IllegalStateException` with a message pointing to `WRAP` and `RandomIDGenerator` as alternatives.

## Consequences

- Default generators never fail on counter overflow, so callers no longer need to handle `IllegalStateException` from the happy path.
- After a wrap, LID values within the same hour are no longer monotonically increasing. Applications that depend on strict within-hour ordering should either stay below 4,096 IDs/hour or use `ID<T>` instead.
- Wrapped LIDs duplicate earlier values from the same hour. The composite primary key in the database catches actual conflicts; duplicates across different composite prefixes are harmless.
- For sustained throughput beyond 4,096/hour, the recommended path remains separate generator instances per scope (via `Builder.copy()`), documented in ADR-013.

## Alternatives rejected

### Random 12-bit counter

Replace the sequential counter with `random.nextInt(4096)`.

Rejected because of the birthday paradox: 12 bits gives only 4,096 possible values per hour, and collision probability reaches 1% at 9 values and 50% at 76, which is far too high for a primary-key component. It also destroys within-hour monotonicity entirely, not just past the overflow point.

### Random offset with monotonic increment

Seed the counter with a random offset on each new hour (the pattern GID uses for UUIDv7). GID can afford this because it has 62 additional random bits in the LSB for cross-generator uniqueness. LID has zero — the counter is the entire non-timestamp portion. A random offset of 256 reduces usable capacity to ~3,840 while providing only weak collision resistance (1/256 chance of identical starting positions). The existing per-scope generator pattern handles multi-generator coordination without sacrificing capacity.

### Lock-free CAS

Replace `synchronized (lidLock)` with an `AtomicLong` CAS loop. Orthogonal to the overflow policy — this is a performance optimization, not a behavioral change. Deferred until benchmarks demonstrate contention at realistic thread counts. ADR-012 already identifies this as a future path.

### Bit reallocation

Change the 20:12 hour:counter split to favour more counter bits. Rejected because this is a wire-format change: existing stored LIDs would decode incorrectly under a new split, and we can't break that contract once users have persisted data.
