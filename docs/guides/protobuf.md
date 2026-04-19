# Using jdentifiers with Protocol Buffers

IDs map to `int64` (64-bit) or a `UUID` message (128-bit) in proto definitions.
No custom serializers are needed — conversion is explicit at the boundary.

## Proto definition

```protobuf
// Reusable 128-bit UUID message. Two fixed64 fields give 18 bytes on
// wire (vs 38 for a string UUID), with no intermediate allocation.
message UUID {
  fixed64 msb = 1;
  fixed64 lsb = 2;
}

message Organization {
  int64 id = 1;
  string name = 2;
}

message SyncEvent {
  UUID global_id = 1;
}
```

## Serialization (Kotlin)

```kotlin
// Building a proto message
val message = Organization.newBuilder()
    .setId(organizationId.asLong())
    .setName("Acme")
    .build()

// Reading from a proto message
val organizationId: ID<Organization> = ID.fromLong(message.id)
```

## Serialization (Java)

```java
// Building
builder.setId(organizationId.asLong());

// Reading
ID<Organization> id = ID.fromLong(message.getId());
```

## GID over proto

GID wraps `java.util.UUID`, which stores most-significant and
least-significant bits as two longs. The `UUID` message maps
directly to this without string parsing or allocation overhead.

```kotlin
// Building
val uuid = syncToken.asUUID()
val event = SyncEvent.newBuilder()
    .setGlobalId(UUID.newBuilder()
        .setMsb(uuid.mostSignificantBits)
        .setLsb(uuid.leastSignificantBits))
    .build()

// Reading
val uuid = java.util.UUID(event.globalId.msb, event.globalId.lsb)
val syncToken: GID<SyncMarker> = GID.fromUuid(uuid)
```

**Why not `string`?** A UUID string is 36 characters (38 bytes on wire
with tag + length). The `fixed64` pair is 18 bytes and avoids parsing.
If human readability in JSON transcoding matters more than wire size,
`string` is a reasonable alternative:

```protobuf
message SyncEvent {
  string global_id = 1;  // 36-char UUID string
}
```

```kotlin
builder.setGlobalId(syncToken.toString())
val syncToken: GID<SyncMarker> = GID.fromString(event.globalId)
```

## LID over proto

LID fits in `int32`:

```protobuf
message Edition {
  int32 id = 1;
}
```

```kotlin
builder.setId(editionId.asInt())
val lid: LID<Edition> = LID.fromInt(message.id)
```
