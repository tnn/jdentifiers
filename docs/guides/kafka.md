# Using jdentifiers with Apache Kafka

## Message keys

ID makes a good Kafka message key. The simplest approach uses Kafka's
built-in `LongSerializer`:

```kotlin
val producer = KafkaProducer<Long, String>(props, LongSerializer(), StringSerializer())
producer.send(ProducerRecord("organizations", organizationId.asLong(), payload))

// Consumer side:
val raw: Long = record.key()
val id: ID<Organization> = ID.fromLong(raw)
```

If you want the key typed as `ID<T>` in the producer/consumer generics,
write a thin wrapper:

```kotlin
class IDSerializer<T : IDAble> : Serializer<ID<T>> {
    override fun serialize(topic: String, data: ID<T>?): ByteArray? {
        data ?: return null
        return ByteBuffer.allocate(8).putLong(data.asLong()).array()
    }
}

class IDDeserializer<T : IDAble> : Deserializer<ID<T>> {
    override fun deserialize(topic: String, data: ByteArray?): ID<T>? {
        data ?: return null
        return ID.fromLong(ByteBuffer.wrap(data).long)
    }
}
```

## Message values (with Protobuf or JSON)

When the message value is serialized with Protobuf or Jackson, use the
corresponding jdentifiers module:

- **Protobuf**: see [protobuf.md](protobuf.md) — `ID.asLong()` into `int64` fields
- **Jackson**: register `JdentifiersHexModule` on your `ObjectMapper`

```kotlin
val mapper = ObjectMapper().registerModule(JdentifiersHexModule())
val producer = KafkaProducer<ID<Organization>, String>(props, IDSerializer(), StringSerializer())

producer.send(ProducerRecord("organizations", organizationId, mapper.writeValueAsString(event)))
```

## Serde wrapper

If you use Kafka Streams or a framework that expects `Serde<T>`:

```kotlin
class IDSerde<T : IDAble> : Serde<ID<T>> {
    override fun serializer() = IDSerializer<T>()
    override fun deserializer() = IDDeserializer<T>()
}
```

## Partition affinity

Because `ID<T>.asLong()` is deterministic, messages keyed by the same ID
always land on the same partition. K-sortable IDs distribute well across
partitions since the lower bits (counter + node) vary across keys.
