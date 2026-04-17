# ADR-009: Clock regression policy

**Status**: Accepted

## Context

System clocks can move backwards due to NTP corrections, VM migrations, or leap second adjustments. A time-sorted ID
generator must handle this without producing out-of-order or duplicate IDs.

## Decision

Three-tier policy:

| Regression           | ID / GID (ms precision)                                          | LID (hour precision)                                                                             |
|----------------------|------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| ≤ 1 second           | Spin-wait until the clock catches up to the last seen timestamp. | N/A — ms-level regression within the same hour bucket is invisible (counter keeps incrementing). |
| > 1 second           | Throw `IllegalStateException`.                                   | N/A at ms level.                                                                                 |
| Across hour boundary | N/A                                                              | Throw `IllegalStateException`.                                                                   |

## Consequence

Small NTP corrections (typically < 500ms) are absorbed transparently. Large regressions (manual `date --set`, VM
snapshot restore) are surfaced immediately as exceptions rather than silently producing mis-ordered IDs. The 1-second
threshold is a pragmatic choice: NTP slew corrections on well-configured systems stay well below this, while anything
larger indicates a configuration error that the operator should investigate.
