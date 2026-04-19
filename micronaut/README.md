# jdentifiers-micronaut

[Micronaut](https://micronaut.io/) type converters for `ID<T>`, `LID<T>` and `GID<T>`.

Registers bidirectional converters with Micronaut's `ConversionService`,
enabling ID types in route parameters, configuration values, and anywhere
the framework performs automatic type conversion.

## Conversions

| From / To   | `ID<T>`      | `LID<T>`     | `GID<T>`     |
|-------------|--------------|--------------|--------------|
| `Long`      | ↔            |              |              |
| `Integer`   |              | ↔            |              |
| `UUID`      |              |              | ↔            |
| `String`    | ↔ (hex)      | ↔ (hex)      | ↔ (UUID)     |

String representations use lowercase hex for ID/LID and standard UUID
format for GID, consistent with the Jackson module.

## Usage

The converters are discovered automatically via `TypeConverterRegistrar`.
No configuration needed beyond adding the dependency.

```java
// Route parameter binding
@Get("/organizations/{id}")
Organization get(@PathVariable ID<Organization> id) { ... }

// Configuration value injection
@Value("${app.default-org-id}")
ID<Organization> defaultOrgId;
```

## Maven

```xml
<dependency>
  <groupId>dk.ceti.jdentifiers</groupId>
  <artifactId>jdentifiers-micronaut</artifactId>
  <version>${jdentifiers.version}</version>
</dependency>
```

`micronaut-core` is a `provided` dependency.
