# ADR-004: Opaque types, strategy in the generator

**Status**: Accepted

## Context

Some libraries embed generation metadata (epoch, version bits, node IDs) into the ID type itself, coupling the type to
its generation strategy.

## Decision

`ID`, `LID`, and `GID` are opaque wrappers around a numeric value. They expose `fromLong`/`fromInt`/`fromUuid`
factory methods and corresponding accessors. All bit-layout knowledge lives in `IDGenerator` implementations. The types
do not know — and cannot know — whether their bits came from `RandomIDGenerator`, `KSortableIDGenerator`, or were
deserialized from a database column.

## Consequence

Generation strategy can be changed per entity type without modifying consuming code. A system can use k-sortable
generation for `ID<Order>` (where insertion-order matters for B-tree locality) and random generation for `ID<ApiKey>` (
where predictability is a security risk) — both produce the same `ID<T>` type. The tradeoff is that the types cannot
self-describe their layout; the `KSortableIDDebugger` utility exists to fill this gap for operational debugging.
