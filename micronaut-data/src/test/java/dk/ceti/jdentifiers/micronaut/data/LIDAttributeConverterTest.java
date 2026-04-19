package dk.ceti.jdentifiers.micronaut.data;

import dk.ceti.jdentifiers.id.LID;
import io.micronaut.core.convert.ConversionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LIDAttributeConverterTest {

    private final LIDAttributeConverter converter = new LIDAttributeConverter();

    @Test
    void round_trips() {
        var original = LID.fromInt(42);
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
    void boundary_values() {
        for (var v : new int[]{0, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            var lid = LID.fromInt(v);
            assertEquals(v, converter.convertToPersistedValue(lid, ConversionContext.DEFAULT));
            assertEquals(lid, converter.convertToEntityValue(v, ConversionContext.DEFAULT));
        }
    }
}
