# ADR-005: Custom epoch 2020-01-01

**Status**: Accepted

## Context

Using Unix epoch (1970) for a 42-bit ms timestamp wastes ~50 years of range on the past. A custom epoch closer to the
library's creation maximizes usable future range.

## Decision

`KSortableIDGenerator` uses 2020-01-01T00:00:00Z as the epoch for `ID` and `LID` timestamps. GID uses Unix epoch as
mandated by RFC 9562.

## Consequence

ID timestamps roll over in 2159 (vs. 2039 with Unix epoch) — an additional 120 years. The epoch constant (
`1_577_836_800_000L`) is baked into the generator and cannot be changed without breaking sort ordering of existing IDs.
For LID, the epoch is configurable per generator instance to allow newer deployments to extend their range.
