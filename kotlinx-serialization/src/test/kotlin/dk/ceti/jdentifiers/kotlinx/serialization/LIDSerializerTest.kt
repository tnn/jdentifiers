package dk.ceti.jdentifiers.kotlinx.serialization

import dk.ceti.jdentifiers.id.IDAble
import dk.ceti.jdentifiers.id.LID
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class LIDSerializerTest {

    private val json = Json

    @Test
    fun `serialize LID to hex string`() {
        val lid: LID<IDAble> = LID.fromString("6a677fc2")
        val result = json.encodeToString(LIDSerializer, lid)
        assertEquals("\"6a677fc2\"", result)
    }

    @Test
    fun `deserialize hex string to LID`() {
        val lid = json.decodeFromString(LIDSerializer, "\"6a677fc2\"")
        assertEquals(1785167810, lid.asInt())
    }

    @Test
    fun `round-trip LID`() {
        val original: LID<IDAble> = LID.fromInt(-847366369)
        val encoded = json.encodeToString(LIDSerializer, original)
        val decoded = json.decodeFromString(LIDSerializer, encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `deserialize upper-case hex`() {
        val lid = json.decodeFromString(LIDSerializer, "\"6A677FC2\"")
        assertEquals(LID.fromString("6a677fc2"), lid)
    }

    @Test
    fun `deserialize mixed-case hex`() {
        val lid = json.decodeFromString(LIDSerializer, "\"6a677FC2\"")
        assertEquals(LID.fromString("6a677fc2"), lid)
    }

    @Test
    fun `deserialize invalid hex throws SerializationException`() {
        val ex = assertFailsWith<SerializationException> {
            json.decodeFromString(LIDSerializer, "\"xyz\"")
        }
        assertIs<IllegalArgumentException>(ex.cause)
    }

    @Test
    fun `deserialize empty string throws SerializationException`() {
        val ex = assertFailsWith<SerializationException> {
            json.decodeFromString(LIDSerializer, "\"\"")
        }
        assertIs<IllegalArgumentException>(ex.cause)
    }
}
