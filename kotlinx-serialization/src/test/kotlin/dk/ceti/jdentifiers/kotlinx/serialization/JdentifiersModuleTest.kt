package dk.ceti.jdentifiers.kotlinx.serialization

import dk.ceti.jdentifiers.id.GID
import dk.ceti.jdentifiers.id.ID
import dk.ceti.jdentifiers.id.IDAble
import dk.ceti.jdentifiers.id.LID
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class Organization : IDAble
class User : IDAble

class JdentifiersModuleTest {

    @Serializable
    data class Entity(
        @Contextual val id: ID<IDAble>,
        @Contextual val gid: GID<IDAble>,
        @Contextual val lid: LID<IDAble>,
    )

    @Serializable
    data class NullableEntity(
        @Contextual val id: ID<IDAble>?,
        @Contextual val gid: GID<IDAble>?,
        @Contextual val lid: LID<IDAble>?,
    )

    private val json = Json {
        serializersModule = jdentifiersSerializersModule
    }

    @Test
    fun `serialize entity with all ID types`() {
        val entity = Entity(
            id = ID.fromString("6a677fc2ee05e1f6"),
            gid = GID.fromUuid(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
            lid = LID.fromString("6a677fc2"),
        )
        val result = json.encodeToString(Entity.serializer(), entity)
        assertEquals(
            """{"id":"6a677fc2ee05e1f6","gid":"550e8400-e29b-41d4-a716-446655440000","lid":"6a677fc2"}""",
            result,
        )
    }

    @Test
    fun `deserialize entity with all ID types`() {
        val jsonStr = """{"id":"6a677fc2ee05e1f6","gid":"550e8400-e29b-41d4-a716-446655440000","lid":"6a677fc2"}"""
        val entity = json.decodeFromString(Entity.serializer(), jsonStr)
        assertEquals(7667237365815304694L, entity.id.asLong())
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), entity.gid.asUUID())
        assertEquals(1785167810, entity.lid.asInt())
    }

    @Test
    fun `round-trip entity`() {
        val entity = Entity(
            id = ID.fromString("6a677fc2ee05e1f6"),
            gid = GID.fromUuid(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
            lid = LID.fromString("6a677fc2"),
        )
        val encoded = json.encodeToString(Entity.serializer(), entity)
        val decoded = json.decodeFromString(Entity.serializer(), encoded)
        assertEquals(entity.id, decoded.id)
        assertEquals(entity.gid, decoded.gid)
        assertEquals(entity.lid, decoded.lid)
    }

    @Test
    fun `null json value for non-nullable ID field throws`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString(
                Entity.serializer(),
                """{"id":null,"gid":"550e8400-e29b-41d4-a716-446655440000","lid":"6a677fc2"}"""
            )
        }
    }

    @Test
    fun `nullable fields deserialize null`() {
        val entity = json.decodeFromString(NullableEntity.serializer(), """{"id":null,"gid":null,"lid":null}""")
        assertNull(entity.id)
        assertNull(entity.gid)
        assertNull(entity.lid)
    }

    @Test
    fun `nullable fields deserialize values`() {
        val entity = json.decodeFromString(
            NullableEntity.serializer(),
            """{"id":"6a677fc2ee05e1f6","gid":"550e8400-e29b-41d4-a716-446655440000","lid":"6a677fc2"}""",
        )
        assertEquals(7667237365815304694L, entity.id!!.asLong())
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), entity.gid!!.asUUID())
        assertEquals(1785167810, entity.lid!!.asInt())
    }

    @Test
    fun `round-trip nullable entity with nulls`() {
        val entity = NullableEntity(id = null, gid = null, lid = null)
        val encoded = json.encodeToString(NullableEntity.serializer(), entity)
        val decoded = json.decodeFromString(NullableEntity.serializer(), encoded)
        assertNull(decoded.id)
        assertNull(decoded.gid)
        assertNull(decoded.lid)
    }

    @Test
    fun `round-trip ID with typed IDAble subtype`() {
        val userId: ID<User> = ID.fromString("6a677fc2ee05e1f6")
        val orgId: ID<Organization> = ID.fromString("8a677fc2ee05e1f6")

        val userIdJson = json.encodeToString(IDSerializer, ID.cast(userId))
        val orgIdJson = json.encodeToString(IDSerializer, ID.cast(orgId))

        val decodedUserId: ID<User> = ID.cast(json.decodeFromString(IDSerializer, userIdJson))
        val decodedOrgId: ID<Organization> = ID.cast(json.decodeFromString(IDSerializer, orgIdJson))

        assertEquals(userId, decodedUserId)
        assertEquals(orgId, decodedOrgId)
    }

    @Test
    fun `round-trip GID with typed IDAble subtype`() {
        val userGid: GID<User> = GID.fromUuid(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))

        val encoded = json.encodeToString(GIDSerializer, GID.cast(userGid))
        val decoded: GID<User> = GID.cast(json.decodeFromString(GIDSerializer, encoded))

        assertEquals(userGid, decoded)
    }

    @Test
    fun `round-trip LID with typed IDAble subtype`() {
        val userLid: LID<User> = LID.fromString("6a677fc2")

        val encoded = json.encodeToString(LIDSerializer, LID.cast(userLid))
        val decoded: LID<User> = LID.cast(json.decodeFromString(LIDSerializer, encoded))

        assertEquals(userLid, decoded)
    }

    @Test
    fun `ID as map key`() {
        val id: ID<IDAble> = ID.fromString("6a677fc2ee05e1f6")
        val map = mapOf(id to "user")
        val serializer = MapSerializer(IDSerializer, String.serializer())
        val encoded = json.encodeToString(serializer, map)
        assertEquals("""{"6a677fc2ee05e1f6":"user"}""", encoded)
        val decoded = json.decodeFromString(serializer, encoded)
        assertEquals(map, decoded)
    }

    @Test
    fun `GID as map key`() {
        val gid: GID<IDAble> = GID.fromUuid(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        val map = mapOf(gid to "user")
        val serializer = MapSerializer(GIDSerializer, String.serializer())
        val encoded = json.encodeToString(serializer, map)
        assertEquals("""{"550e8400-e29b-41d4-a716-446655440000":"user"}""", encoded)
        val decoded = json.decodeFromString(serializer, encoded)
        assertEquals(map, decoded)
    }

    @Test
    fun `LID as map key`() {
        val lid: LID<IDAble> = LID.fromString("6a677fc2")
        val map = mapOf(lid to "team")
        val serializer = MapSerializer(LIDSerializer, String.serializer())
        val encoded = json.encodeToString(serializer, map)
        assertEquals("""{"6a677fc2":"team"}""", encoded)
        val decoded = json.decodeFromString(serializer, encoded)
        assertEquals(map, decoded)
    }
}
