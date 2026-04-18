package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.ceti.jdentifiers.id.GID;
import dk.ceti.jdentifiers.id.ID;
import dk.ceti.jdentifiers.id.IDAble;
import dk.ceti.jdentifiers.id.LID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdentifiersHexModuleTest {
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupSpec() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JdentifiersHexModule());
    }

    // --- ID tests ---

    @Test
    void serializeID() throws JsonProcessingException {
        ID<IDAble> id = ID.fromString("6a677fc2ee05e1f6");
        String json = mapper.writeValueAsString(id);
        assertEquals("\"6a677fc2ee05e1f6\"", json);
    }

    @Test
    void deserializeValidID() throws JsonProcessingException {
        ID<?> id = mapper.readValue("\"6a677fc2ee05e1f6\"", ID.class);
        assertEquals(7667237365815304694L, id.asLong());
    }

    @Test
    void deserializeUpperCaseID() throws JsonProcessingException {
        ID<?> id = mapper.readValue("\"6A677FC2EE05E1F6\"", ID.class);
        assertEquals(ID.fromString("6a677fc2ee05e1f6"), id);
    }

    @Test
    void deserializeMixedCaseID() throws JsonProcessingException {
        ID<?> id = mapper.readValue("\"6a677FC2ee05E1f6\"", ID.class);
        assertEquals(ID.fromString("6a677fc2ee05e1f6"), id);
    }

    @Test
    void deserializeEmptyStringID() {
        var ex = assertThrows(JsonProcessingException.class, () ->
            mapper.readValue("\"\"", ID.class)
        );
        assertTrue(ex.getMessage().contains("Invalid ID format"));
    }

    @Test
    void deserializeInvalidID() {
        var ex = assertThrows(JsonProcessingException.class, () ->
            mapper.readValue("\"xyz\"", ID.class)
        );
        assertTrue(ex.getMessage().contains("Invalid ID format"));
    }

    // --- GID tests ---

    @Test
    void serializeGID() throws JsonProcessingException {
        GID<IDAble> gid = GID.fromString("420bb7c1-4bb6-4936-9ab1-b6b81f9c0f61");
        String json = mapper.writeValueAsString(gid);
        assertEquals("\"420bb7c1-4bb6-4936-9ab1-b6b81f9c0f61\"", json);
    }

    @Test
    void deserializeValidGID() throws JsonProcessingException {
        GID<?> gid = mapper.readValue("\"420bb7c1-4bb6-4936-9ab1-b6b81f9c0f61\"", GID.class);
        assertEquals("420bb7c1-4bb6-4936-9ab1-b6b81f9c0f61", gid.asUUID().toString());
    }

    @Test
    void deserializeUpperCaseGID() throws JsonProcessingException {
        GID<?> gid = mapper.readValue("\"420BB7C1-4BB6-4936-9AB1-B6B81F9C0F61\"", GID.class);
        assertEquals(GID.fromString("420bb7c1-4bb6-4936-9ab1-b6b81f9c0f61"), gid);
    }

    @Test
    void deserializeMixedCaseGID() throws JsonProcessingException {
        GID<?> gid = mapper.readValue("\"420bb7C1-4BB6-4936-9ab1-B6B81f9c0f61\"", GID.class);
        assertEquals(GID.fromString("420bb7c1-4bb6-4936-9ab1-b6b81f9c0f61"), gid);
    }

    @Test
    void deserializeInvalidGID() {
        var ex = assertThrows(JsonProcessingException.class, () ->
            mapper.readValue("\"not-a-uuid\"", GID.class)
        );
        assertTrue(ex.getMessage().contains("Invalid GID format"));
    }

    // --- LID tests ---

    @Test
    void serializeLID() throws JsonProcessingException {
        LID<IDAble> lid = LID.fromString("6a677fc2");
        String json = mapper.writeValueAsString(lid);
        assertEquals("\"6a677fc2\"", json);
    }

    @Test
    void deserializeValidLID() throws JsonProcessingException {
        LID<?> lid = mapper.readValue("\"6a677fc2\"", LID.class);
        assertEquals(0x6a677fc2, lid.toInteger());
    }

    @Test
    void deserializeUpperCaseLID() throws JsonProcessingException {
        LID<?> lid = mapper.readValue("\"6A677FC2\"", LID.class);
        assertEquals(LID.fromString("6a677fc2"), lid);
    }

    @Test
    void deserializeMixedCaseLID() throws JsonProcessingException {
        LID<?> lid = mapper.readValue("\"6a677FC2\"", LID.class);
        assertEquals(LID.fromString("6a677fc2"), lid);
    }

    @Test
    void deserializeInvalidLID() {
        var ex = assertThrows(JsonProcessingException.class, () ->
            mapper.readValue("\"zz\"", LID.class)
        );
        assertTrue(ex.getMessage().contains("Invalid LID format"));
    }

    // --- Null deserialization ---

    @Test
    void deserializeNullID() throws JsonProcessingException {
        assertNull(mapper.readValue("null", ID.class));
    }

    @Test
    void deserializeNullGID() throws JsonProcessingException {
        assertNull(mapper.readValue("null", GID.class));
    }

    @Test
    void deserializeNullLID() throws JsonProcessingException {
        assertNull(mapper.readValue("null", LID.class));
    }

    // --- Empty-string deserialization ---

    @Test
    void deserializeEmptyStringGID() {
        var ex = assertThrows(JsonProcessingException.class, () ->
            mapper.readValue("\"\"", GID.class)
        );
        assertTrue(ex.getMessage().contains("Invalid GID format"));
    }

    @Test
    void deserializeEmptyStringLID() {
        var ex = assertThrows(JsonProcessingException.class, () ->
            mapper.readValue("\"\"", LID.class)
        );
        assertTrue(ex.getMessage().contains("Invalid LID format"));
    }

    // --- POJO field round-trip ---

    record IdHolder(ID<?> id, GID<?> gid, LID<?> lid) {
    }

    @Test
    void pojoFieldRoundTrip() throws JsonProcessingException {
        IdHolder holder = new IdHolder(
            ID.fromString("6a677fc2ee05e1f6"),
            GID.fromString("420bb7c1-4bb6-4936-9ab1-b6b81f9c0f61"),
            LID.fromString("6a677fc2")
        );

        String json = mapper.writeValueAsString(holder);
        IdHolder deserialized = mapper.readValue(json, IdHolder.class);

        assertEquals(holder.id(), deserialized.id());
        assertEquals(holder.gid(), deserialized.gid());
        assertEquals(holder.lid(), deserialized.lid());
    }

    @Test
    void pojoFieldNullRoundTrip() throws JsonProcessingException {
        IdHolder holder = new IdHolder(null, null, null);
        String json = mapper.writeValueAsString(holder);
        IdHolder deserialized = mapper.readValue(json, IdHolder.class);

        assertNull(deserialized.id());
        assertNull(deserialized.gid());
        assertNull(deserialized.lid());
    }
}
