package dk.ceti.jdentifiers.micronaut;

import dk.ceti.jdentifiers.id.GID;
import io.micronaut.core.convert.MutableConversionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GIDTypeConverterTest {

    private static final MutableConversionService cs =
        MutableConversionService.create();

    @BeforeAll
    static void register() {
        new JdentifiersHexConverterRegistrar().register(cs);
    }

    @Test
    void uuid_to_gid() {
        var uuid = UUID.fromString("01234567-89ab-7def-8123-456789abcdef");
        var result = cs.convert(uuid, GID.class);
        assertTrue(result.isPresent());
        assertEquals(uuid, result.get().asUUID());
    }

    @Test
    void gid_to_uuid() {
        var uuid = UUID.fromString("01234567-89ab-7def-8123-456789abcdef");
        var gid = GID.fromUuid(uuid);
        var result = cs.convert(gid, UUID.class);
        assertTrue(result.isPresent());
        assertEquals(uuid, result.get());
    }

    @Test
    void gid_to_string() {
        var gid = GID.fromUuid(UUID.fromString("01234567-89ab-7def-8123-456789abcdef"));
        var result = cs.convert(gid, String.class);
        assertTrue(result.isPresent());
        assertEquals("01234567-89ab-7def-8123-456789abcdef", result.get());
    }

    @Test
    void string_to_gid() {
        var result = cs.convert("01234567-89ab-7def-8123-456789abcdef", GID.class);
        assertTrue(result.isPresent());
        assertEquals("01234567-89ab-7def-8123-456789abcdef", result.get().toString());
    }

    @Test
    void invalid_string_returns_empty() {
        var result = cs.convert("not-a-uuid", GID.class);
        assertTrue(result.isEmpty());
    }
}
