# ADR-010: Counter overflow policy — block vs. throw

**Status**: Accepted (LID section superseded by [ADR-014](adr-014-lid-overflow-wrap-default.md))

## Context

When the monotonic counter within a time tick is exhausted, the generator must choose between blocking (waiting for the next tick) and throwing.

## Decision

`ID` and `GID` **block** (spin-wait via `Thread.onSpinWait()`) until the next millisecond. `LID` **throws** `IllegalStateException`.

## Consequence

For ID/GID, the caller experiences at most ~1ms of added latency, which is acceptable for a millisecond-precision generator. For LID, blocking would mean waiting up to 1 hour, which is never acceptable. An overflow at hour precision means the caller is generating far more LIDs per scope than the 32-bit layout supports — this is a design error, not a transient condition, and should fail loudly.
