package dk.ceti.jdentifiers.micronaut.data;

import dk.ceti.jdentifiers.id.LID;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.data.model.runtime.convert.AttributeConverter;

/**
 * Converts {@link LID} to {@code Integer} for persistence and back.
 */
public class LIDAttributeConverter implements AttributeConverter<LID<?>, Integer> {

    @Override
    public Integer convertToPersistedValue(LID<?> entityValue, ConversionContext context) {
        return entityValue != null ? entityValue.asInt() : null;
    }

    @Override
    public LID<?> convertToEntityValue(Integer persistedValue, ConversionContext context) {
        return persistedValue != null ? LID.fromInt(persistedValue) : null;
    }
}
