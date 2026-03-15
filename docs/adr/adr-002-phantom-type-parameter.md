# ADR-002: Phantom type parameter `<T extends IDAble>`

**Status**: Accepted

## Context

The most common identifier bug in service code is swapping two IDs of different entity types — passing a user ID where an organization ID is expected. Both are `long` values; the compiler cannot help.

## Decision

All ID types carry a phantom type parameter bounded by `IDAble`, a marker interface. The type parameter exists only at compile time (erased at runtime). `equals` and `hashCode` are based on the underlying value only, ignoring `T`, because the JVM cannot distinguish `ID<User>` from `ID<Organization>` after erasure.

## Consequence

`ID<User>` and `ID<Organization>` are assignment-incompatible at compile time. An `ID.cast()` escape hatch exists for the rare cases where the caller knows better than the compiler (e.g. deserialization). Runtime cost is zero.
