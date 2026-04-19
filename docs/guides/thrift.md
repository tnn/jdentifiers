# Using jdentifiers with Apache Thrift

IDs map to `i64` (64-bit) or `string` (128-bit UUID) in Thrift IDL.

## Thrift definition

```thrift
struct Organization {
  1: required i64 id
  2: required string name
}

struct Edition {
  1: required i32 id
}
```

## Conversion (Kotlin)

```kotlin
// ID<T>
val organizationId: ID<Organization> = ID.fromLong(thriftOrganization.id)
thriftOrganization.setId(organizationId.asLong())

// LID<T>
val editionId: LID<Edition> = LID.fromInt(thriftEdition.id)
thriftEdition.setId(editionId.asInt())

// GID<T> — store as string field
val syncToken: GID<SyncMarker> = GID.fromString(thriftEvent.globalId)
thriftEvent.setGlobalId(syncToken.toString())
```

## Java

```java
ID<Organization> id = ID.fromLong(thriftOrganization.getId());
thriftOrganization.setId(id.asLong());
```

The pattern is the same as Protobuf: explicit conversion at the service boundary,
typed IDs inside the application.
