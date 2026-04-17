package dk.ceti.jdentifiers.kotlinx.serialization

import dk.ceti.jdentifiers.id.ID
import dk.ceti.jdentifiers.id.IDAble
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class IDSerializerTest {

    private val json = Json

    @Test
    fun `serialize ID to hex string`() {
        val id: ID<IDAble> = ID.fromString("6a677fc2ee05e1f6")
        val result = json.encodeToString(IDSerializer, id)
        assertEquals("\"6a677fc2ee05e1f6\"", result)
    }

    @Test
    fun `deserialize hex string to ID`() {
        val id = json.decodeFromString(IDSerializer, "\"6a677fc2ee05e1f6\"")
        assertEquals(7667237365815304694L, id.asLong())
    }

    @Test
    fun `round-trip ID`() {
        val original: ID<IDAble> = ID.fromLong(-8473663698680552970L)
        val encoded = json.encodeToString(IDSerializer, original)
        val decoded = json.decodeFromString(IDSerializer, encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `deserialize upper-case hex`() {
        val id = json.decodeFromString(IDSerializer, "\"6A677FC2EE05E1F6\"")
        assertEquals(ID.fromString("6a677fc2ee05e1f6"), id)
    }

    @Test
    fun `deserialize invalid hex throws SerializationException`() {
        val ex = assertFailsWith<SerializationException> {
            json.decodeFromString(IDSerializer, "\"xyz\"")
        }
        assertIs<IllegalArgumentException>(ex.cause)
    }

    @Test
    fun `deserialize empty string throws SerializationException`() {
        val ex = assertFailsWith<SerializationException> {
            json.decodeFromString(IDSerializer, "\"\"")
        }
        assertIs<IllegalArgumentException>(ex.cause)
    }
}
