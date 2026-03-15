# ADR-001: Unsigned comparison everywhere

**Status**: Accepted

## Context

Java's `long` and `int` are signed. Snowflake-style layouts historically reserved bit 63 as a sign bit to avoid negative values confusing signed comparison. This wastes one bit of timestamp range.

## Decision

All three ID types use unsigned comparison (`Long.compareUnsigned`, `Integer.compareUnsigned`). `GID.compareTo` deliberately diverges from `UUID.compareTo` on JDK < 20, which used signed comparison (corrected in JDK 20+ via [JDK-7025832](https://bugs.openjdk.org/browse/JDK-7025832)).

## Consequence

Bit 63 is available for timestamp data, giving `ID` a full 42-bit timestamp (139 years) instead of 41 bits (69 years). Code that compares IDs via raw `long` comparison (e.g. `id1.asLong() < id2.asLong()`) will get wrong results for values with bit 63 set — callers must use `compareTo` or `Long.compareUnsigned`. This is documented on the type and is consistent with how `java.time.Instant` and other JDK types handle unsigned semantics.
