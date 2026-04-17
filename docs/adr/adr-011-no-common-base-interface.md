# ADR-011: No common base interface for ID types

**Status**: Accepted

## Context

`ID`, `LID`, and `GID` share similar patterns (factory methods, `Comparable`, `Serializable`) but differ in their
underlying primitive type and API surface.

## Decision

The three types share no common interface or abstract base class.

## Consequence

There is no `Identifier<T>` supertype. Code that needs to handle multiple ID types must do so explicitly (e.g.,
overloaded methods). This is intentional: a generic `Identifier` interface would either be too broad (exposing
`asLong()` on a `GID`) or too narrow (only `toString()` and `compareTo`, which `Object` and `Comparable` already
provide). The phantom type parameter already prevents most mix-up bugs; adding a shared supertype would add complexity
without preventing new categories of errors.
