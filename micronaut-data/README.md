# jdentifiers-micronaut-data

[Micronaut Data](https://micronaut-projects.github.io/micronaut-data/latest/guide/)
`AttributeConverter` implementations for `ID<T>`, `LID<T>` and `GID<T>`.

## Attribute converters

| Converter               | Entity type | Persisted type |
|-------------------------|-------------|----------------|
| `IDAttributeConverter`  | `ID<T>`     | `Long`         |
| `LIDAttributeConverter` | `LID<T>`    | `Integer`      |
| `GIDAttributeConverter` | `GID<T>`    | `UUID`         |

## Meta-annotations

To avoid repeating `@MappedProperty(converter = ...)` and `@TypeDef` on
every field, this module provides shorthand annotations:

| Annotation   | Equivalent to                                                        |
|--------------|----------------------------------------------------------------------|
| `@MappedId`  | `@MappedProperty(converter = IDAttributeConverter.class)` + `LONG`   |
| `@MappedLid` | `@MappedProperty(converter = LIDAttributeConverter.class)` + `INTEGER` |
| `@MappedGid` | `@MappedProperty(converter = GIDAttributeConverter.class)` + `UUID`  |

## Entity example

```java
@MappedEntity("organizations")
public class Organization {

    @Id
    @MappedId
    private ID<Organization> id;

    private String name;

    // getters, setters
}
```

No `@GeneratedValue` — the ID is supplied by the application before
calling `save()`:

```java
var org = new Organization();
org.setId(generator.identifier());
org.setName("Acme");
repository.save(org);
```

## Composite keys

For scoped identifiers like `LID<Team>` within an organization,
use `@EmbeddedId`:

```java
@Embeddable
public class TeamKey {

    @MappedProperty(value = "organization_id", converter = IDAttributeConverter.class)
    @TypeDef(type = DataType.LONG)
    private ID<Organization> organizationId;

    @MappedProperty(value = "team_id", converter = LIDAttributeConverter.class)
    @TypeDef(type = DataType.INTEGER)
    private LID<Team> teamId;

    // getters, setters, equals, hashCode
}

@MappedEntity("teams")
public class Team {

    @EmbeddedId
    private TeamKey id;

    private String name;
}
```

## Maven

```xml
<dependency>
  <groupId>dk.ceti.jdentifiers</groupId>
  <artifactId>jdentifiers-micronaut-data</artifactId>
  <version>${jdentifiers.version}</version>
</dependency>
```

`micronaut-data-model` is a `provided` dependency.
For HTTP parameter binding, also add `jdentifiers-micronaut`.
