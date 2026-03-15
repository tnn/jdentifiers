# ADR-008: `SecureRandom` provider selection

**Status**: Accepted (revised)

## Context

The JDK default `SecureRandom` on Linux/macOS (NativePRNG) reads from `/dev/random` and can block when the OS entropy pool is exhausted. In high-throughput ID generation, this causes unpredictable latency spikes.

## Decision

Both `RandomIDGenerator` and `KSortableIDGenerator` use a shared `SecureRandoms.create()` factory that tries providers in order: **DRBG**, then **SHA1PRNG**, then the platform default. DRBG (available on JDK 9+) is preferred because it periodically reseeds from hardware entropy while still providing predictable latency. SHA1PRNG is the fallback for environments where DRBG is unavailable.

## Consequence

Predictable, non-blocking latency with stronger randomness than SHA1PRNG-only (DRBG reseeds periodically). Identifier generation does not require cryptographic-strength randomness — it requires collision resistance, which both DRBG and SHA1PRNG provide. Exception: `GID` via `RandomIDGenerator` delegates to `UUID.randomUUID()`, which uses the JDK default `SecureRandom`, because `UUID.randomUUID()` is the canonical way to produce correct UUIDv4 variant/version bits.

## Revision (2026-03-15)

Changed from SHA1PRNG-first to DRBG-first. The original decision predated the addition of `KSortableIDGenerator`, which benefits from DRBG's periodic reseeding for the random LSB in UUIDv7 generation. Both generators now share the same provider selection logic via `SecureRandoms`.
