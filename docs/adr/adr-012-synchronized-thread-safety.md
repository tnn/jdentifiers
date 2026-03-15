# ADR-012: Thread safety via per-type `synchronized` locks

**Status**: Accepted (revised). Subject to future revision based on benchmark data.

## Context

`KSortableIDGenerator` maintains mutable state (last timestamp, counter) that must be updated atomically. The hot path is `Clock.millis()` + counter increment + bit assembly.

## Decision

Each generation method is guarded by a dedicated lock (`idLock`, `gidLock`, `lidLock`), so a spin-wait in one method cannot block the others.

## Consequence

Simple, correct, auditable. The lock is held for nanoseconds under normal operation (dominated by the `Clock.millis()` system call). Under extreme contention (thousands of threads sharing one generator), each lock type bottlenecks independently. A lock-free `AtomicLong` CAS loop is a known optimization path if benchmarks demonstrate this is a real bottleneck. For now, simplicity is preferred.

**Known limitation**: The lock is held during spin-waits (counter overflow and clock regression recovery). This is intentional — the lock protects the counter state that the spin-wait is waiting to reset. During a spin-wait, other threads calling the same ID type will block, but threads calling different ID types are unaffected.

## Revision (2026-03-15)

Changed from a single `synchronized(this)` lock to per-type locks (`idLock`, `gidLock`, `lidLock`). The original single-lock design caused cross-type head-of-line blocking: a GID counter overflow spin-wait would block unrelated ID and LID generation. The `SecureRandom.nextLong()` call for the GID random LSB was also moved outside the lock to avoid blocking on potential entropy exhaustion.
