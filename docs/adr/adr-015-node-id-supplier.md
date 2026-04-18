# ADR-015: NodeIdSupplier replaces separate nodeBits/nodeId/nodeIdFactory

**Status**: Accepted

## Context

The builder previously had three methods for node configuration: `nodeBits(int)`, `nodeId(int)`, and
`nodeIdFactory(IntSupplier)`. The bit width had to match between the builder and the factory output, but nothing in
the type system enforced this. As we added environment-specific strategies (MAC address, hostname hashing, K8s pod
name parsing), each needing the bit width to mask its output, the mismatch risk grew.

## Decision

Replace the three methods with `nodeId(NodeIdSupplier)`. The interface has two methods:

```java
public interface NodeIdSupplier {
    int nodeBits();
    int nodeId();
}
```

Built-in strategies are static factory methods on `NodeIdStrategies`:

- `of(nodeBits, nodeId)` -- fixed value
- `macAddress(nodeBits)` -- rightmost bytes of first non-loopback NIC
- `random(nodeBits)` -- `SecureRandom`
- `hostname(nodeBits)` -- SHA-256 of hostname
- `kubernetes(nodeBits)` -- K8s Deployment pod suffix decode with SHA-256 fallback
- `auto(nodeBits)` -- cascading: MAC, K8s, hostname, random

All resolve eagerly and return immutable suppliers. `auto()` never throws.

Two abstract methods means `NodeIdSupplier` is not a `@FunctionalInterface`. That is intentional: the bit width and
the value are a pair, not independent concerns.

### Why not keep IntSupplier

`IntSupplier` carries the value but not the bit width. Every strategy would need `nodeBits` as both a parameter (for
masking) and a separate builder call (for configuration), which is exactly the duplication we want to eliminate.

### Why not an enum

Discoverable but not extensible. Custom infrastructure (Consul, AWS IMDS, Postgres advisory locks) requires user-defined
implementations.

## Consequences

- Breaking change to the builder API. Acceptable pre-1.0.
- The `id` module stays zero-dependency. All built-in strategies use `java.net`, `java.security`, and `java.util.regex`.
- Future strategies (AWS, ZooKeeper, PostgreSQL) can ship as separate modules returning `NodeIdSupplier`.
