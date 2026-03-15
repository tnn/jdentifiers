# Jdentifiers

Type-safe identifier library for Java. Phantom-typed wrappers around numeric values (`long`, `int`, `UUID`) that prevent mixing identifiers for different domain entities at compile time.

## Modules

- **id** — Core identifier types: `ID<T>` (64-bit), `LID<T>` (32-bit), `GID<T>` (128-bit UUID wrapper), `IDGenerator`
- **jackson** — Jackson serializers/deserializers
- **kotlinx-serialization** — kotlinx-serialization `KSerializer` implementations
- **micronaut** — Micronaut type converters
- **benchmarks** — JMH benchmarks

## Build & Test

```sh
mvn test                    # run all tests
mvn -pl id test             # run only core module tests
```

## Benchmarks (JMH)

Benchmark classes are in `benchmarks/src/main/java`. Build the shaded JAR then run via exec-maven-plugin:

```sh
mvn -pl id,benchmarks package -DskipTests
mvn -pl benchmarks exec:exec -Dbenchmark=IDStringBenchmark
mvn -pl benchmarks exec:exec -Dbenchmark=IDThroughputGenerationBenchmark
mvn -pl benchmarks exec:exec -Dbenchmark=IDLatencyGenerationBenchmark
```

## Key Design Decisions

- **Big-endian encoding** — All string representations (hex, Base64) use most-significant-byte-first order. Required for k-sortable/time-sorted identifiers where timestamp bits occupy MSB positions. Hex lexicographic ordering matches unsigned numeric ordering.
- **Unsigned comparison** — `ID`, `LID`, and `GID` all use unsigned comparison (`Long.compareUnsigned`, `Integer.compareUnsigned`). `GID.compareTo` deliberately diverges from `UUID.compareTo` on JDK <20 which uses signed comparison.
- **`Comparable<XID<?>>`** — Wildcard type parameter allows `Collections.sort(List<GID<?>>)` to compile. Binary compatible with `Comparable<XID<T>>` due to erasure.
- **Phantom type parameter** — `T extends IDAble` provides compile-time safety without runtime overhead.
- **`java.io.Serializable`** — All three ID types implement `Serializable` with pinned `serialVersionUID`.

## Java Version

Requires JDK 17+.
