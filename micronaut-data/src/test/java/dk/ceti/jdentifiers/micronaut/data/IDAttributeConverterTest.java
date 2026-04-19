package dk.ceti.jdentifiers.micronaut.data;

import dk.ceti.jdentifiers.id.ID;
import io.micronaut.core.convert.ConversionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IDAttributeConverterTest {

    private final IDAttributeConverter converter = new IDAttributeConverter();

    @Test
    void round_trips() {
        var original = ID.fromLong(42L);
        var persisted = converter.convertToPersistedValue(original, ConversionContext.DEFAULT);
        var restored = converter.convertToEntityValue(persisted, ConversionContext.DEFAULT);
        assertEquals(original, restored);
    }

    @Test
    void null_persisted() {
        assertNull(converter.convertToPersistedValue(null, ConversionContext.DEFAULT));
    }

    @Test
    void null_entity() {
        assertNull(converter.convertToEntityValue(null, ConversionContext.DEFAULT));
    }

    @Test
    void boundary_values() {
        for (var v : new long[]{0L, Long.MAX_VALUE, Long.MIN_VALUE}) {
            var id = ID.fromLong(v);
            assertEquals(v, converter.convertToPersistedValue(id, ConversionContext.DEFAULT));
            assertEquals(id, converter.convertToEntityValue(v, ConversionContext.DEFAULT));
        }
    }
}
