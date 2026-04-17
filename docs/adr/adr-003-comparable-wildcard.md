# ADR-003: `Comparable<XID<?>>` wildcard

**Status**: Accepted

## Context

With `Comparable<ID<T>>`, calling `Collections.sort(List<ID<?>>)` does not compile because `ID<?>` does not satisfy
`Comparable<ID<?>>` — it satisfies `Comparable<ID<capture#1>>`.

## Decision

All three types implement `Comparable` with a wildcard: `Comparable<ID<?>>`, `Comparable<LID<?>>`, `Comparable<GID<?>>`.

## Consequence

Mixed-phantom-type collections sort correctly: `Collections.sort(List<ID<?>>)` compiles. Binary compatible with
`Comparable<ID<T>>` due to erasure. The tradeoff is that `ID<User>` and `ID<Organization>` are mutually comparable at
compile time, which is semantically questionable but practically harmless (they share the same unsigned numeric
ordering).
