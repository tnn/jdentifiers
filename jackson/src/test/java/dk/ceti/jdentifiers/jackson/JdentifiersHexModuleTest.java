package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pkgd.jdentifiers.id.ID;
import org.pkgd.jdentifiers.id.IDAble;

import static org.junit.jupiter.api.Assertions.*;

class JdentifiersHexModuleTest {
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupSpec() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JdentifiersHexModule());
    }

    @Test
    void serializeStringID() throws JsonProcessingException {
        ID<IDAble> id = ID.fromString("6a677fc2ee05e1f6");
        String json = mapper.writeValueAsString(id);
        assertEquals("\"6a677fc2ee05e1f6\"", json);
    }

    @Test
    void deserializeValidHex() throws JsonProcessingException {
        ID<?> id = mapper.readValue("\"6a677fc2ee05e1f6\"", ID.class);
        assertEquals(7667237365815304694L, id.asLong());
    }

    @Test
    void deserializeEmptyString() {
        var ex = assertThrows(JsonProcessingException.class, () ->
            mapper.readValue("\"\"", ID.class)
        );
        assertTrue(ex.getMessage().contains("Illegal ID string: "));
    }

    @Test
    void deserializeInvalidHex() {
        var ex = assertThrows(JsonProcessingException.class, () ->
            mapper.readValue("\"xyz\"", ID.class)
        );
        assertTrue(ex.getMessage().contains("Illegal ID string: xyz"));
    }
}