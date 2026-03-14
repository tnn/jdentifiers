package dk.ceti.jdentifiers.kotlinx.serialization

import dk.ceti.jdentifiers.id.GID
import dk.ceti.jdentifiers.id.IDAble
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class GIDSerializerTest {

    private val json = Json

    @Test
    fun `serialize GID to UUID string`() {
        val gid: GID<IDAble> = GID.fromUuid(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        val result = json.encodeToString(GIDSerializer, gid)
        assertEquals("\"550e8400-e29b-41d4-a716-446655440000\"", result)
    }

    @Test
    fun `deserialize UUID string to GID`() {
        val expected: GID<IDAble> = GID.fromUuid(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        val gid = json.decodeFromString(GIDSerializer, "\"550e8400-e29b-41d4-a716-446655440000\"")
        assertEquals(expected, gid)
    }

    @Test
    fun `round-trip GID`() {
        val original: GID<IDAble> = GID.fromUuid(UUID.randomUUID())
        val encoded = json.encodeToString(GIDSerializer, original)
        val decoded = json.decodeFromString(GIDSerializer, encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `deserialize invalid UUID throws SerializationException`() {
        val ex = assertFailsWith<SerializationException> {
            json.decodeFromString(GIDSerializer, "\"not-a-uuid\"")
        }
        assertIs<IllegalArgumentException>(ex.cause)
    }

    @Test
    fun `deserialize empty string throws SerializationException`() {
        val ex = assertFailsWith<SerializationException> {
            json.decodeFromString(GIDSerializer, "\"\"")
        }
        assertIs<IllegalArgumentException>(ex.cause)
    }
}
