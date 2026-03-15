# ADR-007: `java.io.Serializable` with pinned `serialVersionUID`

**Status**: Accepted

## Context

ID values are frequently stored in HTTP sessions, caches, and message queues that use Java serialization. Changing the class structure would break deserialization of in-flight data during rolling deployments.

## Decision

All three types implement `Serializable` with an explicit, pinned `serialVersionUID`. The UID must never change.

## Consequence

Adding fields to the types is a breaking change that requires a migration strategy. This is acceptable because the types are intentionally minimal (a single `long`, `int`, or `UUID` field) and should not grow.
