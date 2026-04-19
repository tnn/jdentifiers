package dk.ceti.jdentifiers.micronaut.data;

import dk.ceti.jdentifiers.id.ID;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.data.model.runtime.convert.AttributeConverter;

/**
 * Converts {@link ID} to {@code Long} for persistence and back.
 */
public class IDAttributeConverter implements AttributeConverter<ID<?>, Long> {

    @Override
    public Long convertToPersistedValue(ID<?> entityValue, ConversionContext context) {
        return entityValue != null ? entityValue.asLong() : null;
    }

    @Override
    public ID<?> convertToEntityValue(Long persistedValue, ConversionContext context) {
        return persistedValue != null ? ID.fromLong(persistedValue) : null;
    }
}
