package dk.ceti.jdentifiers.micronaut;

import dk.ceti.jdentifiers.id.LID;
import io.micronaut.core.convert.MutableConversionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LIDTypeConverterTest {

    private static final MutableConversionService cs =
        MutableConversionService.create();

    @BeforeAll
    static void register() {
        new JdentifiersHexConverterRegistrar().register(cs);
    }

    @Test
    void integer_to_lid() {
        var result = cs.convert(42, LID.class);
        assertTrue(result.isPresent());
        assertEquals(42, result.get().asInt());
    }

    @Test
    void lid_to_integer() {
        var lid = LID.fromInt(42);
        var result = cs.convert(lid, Integer.class);
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    void lid_to_string() {
        var lid = LID.fromInt(255);
        var result = cs.convert(lid, String.class);
        assertTrue(result.isPresent());
        assertEquals("000000ff", result.get());
    }

    @Test
    void string_to_lid() {
        var result = cs.convert("000000ff", LID.class);
        assertTrue(result.isPresent());
        assertEquals(255, result.get().asInt());
    }

    @Test
    void invalid_string_returns_empty() {
        var result = cs.convert("zzzz", LID.class);
        assertTrue(result.isEmpty());
    }
}
