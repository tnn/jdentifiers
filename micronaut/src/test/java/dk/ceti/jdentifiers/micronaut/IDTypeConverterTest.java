package dk.ceti.jdentifiers.micronaut;

import dk.ceti.jdentifiers.id.ID;
import io.micronaut.core.convert.MutableConversionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IDTypeConverterTest {

    private static final MutableConversionService cs =
        MutableConversionService.create();

    @BeforeAll
    static void register() {
        new JdentifiersHexConverterRegistrar().register(cs);
    }

    @Test
    void long_to_id() {
        var result = cs.convert(42L, ID.class);
        assertTrue(result.isPresent());
        assertEquals(42L, result.get().asLong());
    }

    @Test
    void id_to_long() {
        var id = ID.fromLong(42L);
        var result = cs.convert(id, Long.class);
        assertTrue(result.isPresent());
        assertEquals(42L, result.get());
    }

    @Test
    void id_to_string() {
        var id = ID.fromLong(255L);
        var result = cs.convert(id, String.class);
        assertTrue(result.isPresent());
        assertEquals("00000000000000ff", result.get());
    }

    @Test
    void string_to_id() {
        var result = cs.convert("00000000000000ff", ID.class);
        assertTrue(result.isPresent());
        assertEquals(255L, result.get().asLong());
    }

    @Test
    void invalid_string_returns_empty() {
        var result = cs.convert("not-a-hex-id", ID.class);
        assertTrue(result.isEmpty());
    }
}
