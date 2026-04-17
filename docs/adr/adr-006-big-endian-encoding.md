# ADR-006: Big-endian encoding

**Status**: Accepted

## Context

K-sortable identifiers place timestamp bits in the most-significant positions so that unsigned numeric ordering equals
chronological ordering. For this to extend to string representations, the encoding must be MSB-first.

## Decision

All string representations (hex, Base64) use big-endian / most-significant-byte-first order.

## Consequence

`ID.toString()` produces 16 lowercase hex characters where lexicographic string comparison (`strcmp`) gives the same
result as `Long.compareUnsigned` on the underlying value. This means IDs can be sorted correctly in systems that only
see the string form (e.g., `ORDER BY id_hex` in a database text column). Base64 encoding does *not* preserve sort
order — callers must use `compareTo` for ordering.
