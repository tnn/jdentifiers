package dk.ceti.jdentifiers.micronaut.data;

import dk.ceti.jdentifiers.id.GID;
import io.micronaut.core.convert.ConversionContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GIDAttributeConverterTest {

    private final GIDAttributeConverter converter = new GIDAttributeConverter();

    @Test
    void round_trips() {
        var uuid = UUID.fromString("01234567-89ab-7def-8123-456789abcdef");
        var original = GID.fromUuid(uuid);
        var persisted = converter.convertToPersistedValue(original, ConversionContext.DEFAULT);
        var restored = converter.convertToEntityValue(persisted, ConversionContext.DEFAULT);
        assertEquals(original, restored);
    }

    @Test
    void null_values() {
        assertNull(converter.convertToPersistedValue(null, ConversionContext.DEFAULT));
        assertNull(converter.convertToEntityValue(null, ConversionContext.DEFAULT));
    }

    @Test
    void nil_uuid() {
        var nil = GID.fromUuid(new UUID(0L, 0L));
        var persisted = converter.convertToPersistedValue(nil, ConversionContext.DEFAULT);
        assertEquals(new UUID(0L, 0L), persisted);
        assertEquals(nil, converter.convertToEntityValue(persisted, ConversionContext.DEFAULT));
    }
}
