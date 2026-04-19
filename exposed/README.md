# jdentifiers-exposed

[Exposed](https://github.com/JetBrains/Exposed) `ColumnType` implementations for `ID<T>`, `LID<T>` and `GID<T>`.
Exposed is JetBrains' lightweight SQL library for Kotlin.

## Column types

| Function       | Wraps             | SQL type (dialect-dependent) |
|----------------|-------------------|------------------------------|
| `Table.id()`   | `ID<T>` (64-bit)  | `BIGINT` / `NUMBER(19)` etc  |
| `Table.lid()`  | `LID<T>` (32-bit) | `INTEGER` / `NUMBER(10)` etc |
| `Table.gid()`  | `GID<T>` (UUID)   | `UUID` / `BINARY(16)` etc    |

## Usage

```kotlin
object OrganizationsTable : Table("organizations") {
    val id = id<Organization>("id")
    val name = varchar("name", 255)
    override val primaryKey = PrimaryKey(id)
}

// Insert
transaction {
    OrganizationsTable.insert {
        it[id] = generator.identifier<Organization>()
        it[name] = "Acme Charging"
    }
}

// Query
transaction {
    OrganizationsTable.selectAll()
        .where { OrganizationsTable.id eq organizationId }
        .single()[OrganizationsTable.id]
}
```

## Wiring with Koin

```kotlin
val idModule = module {
    single<IDGenerator> { KSortableIDGenerator() }
}
```

Inject into repositories or services as usual:

```kotlin
class OrganizationRepository(private val generator: IDGenerator) {
    fun create(name: String): ID<Organization> = transaction {
        val id = generator.identifier<Organization>()
        OrganizationsTable.insert {
            it[OrganizationsTable.id] = id
            it[OrganizationsTable.name] = name
        }
        id
    }
}
```

## DAO API

The column types above work with Exposed's DSL API (`Table`, `insert`, `selectAll`).
The DAO API (`Entity`, `EntityClass`, `IdTable`) needs a slightly different setup
because it wraps primary keys in `EntityID<T>`.

Since `EntityID<T>` expects a plain type like `Long`, `ID<T>` can't be used as the
entity ID directly. A reasonable workaround is to extend `LongIdTable` without
auto-increment and pass the generated long value at creation time:

```kotlin
// Use IdTable<Long> directly — LongIdTable's id column is final and auto-incremented
object OrganizationsTable : IdTable<Long>("organizations") {
    override val id = long("id").entityId()
    override val primaryKey = PrimaryKey(id)
    val name = varchar("name", 255)
}

class Organization(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Organization>(OrganizationsTable)
    var name by OrganizationsTable.name
}

// Supply the ID as a Long at creation time
val org = Organization.new(generator.identifier<Org>().asLong()) {
    name = "Acme"
}

// Look up by ID
val found = Organization.findById(orgId.asLong())
```

This means the phantom type safety lives in your repository or service layer
rather than the table definition. If you'd prefer type safety all the way to the
database, the DSL API with the column types from this module may be a better fit.

## Maven

```xml
<dependency>
  <groupId>dk.ceti.jdentifiers</groupId>
  <artifactId>jdentifiers-exposed</artifactId>
  <version>${jdentifiers.version}</version>
</dependency>
```

`exposed-core` is a `provided` dependency — bring your own Exposed version.
Tested against Exposed 1.x. A future Exposed 2.x may require a new major version of this module.
