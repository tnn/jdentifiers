package dk.ceti.jdentifiers.micronaut.data;

import dk.ceti.jdentifiers.id.GID;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.data.model.runtime.convert.AttributeConverter;

import java.util.UUID;

/**
 * Converts {@link GID} to {@code UUID} for persistence and back.
 */
public class GIDAttributeConverter implements AttributeConverter<GID<?>, UUID> {

    @Override
    public UUID convertToPersistedValue(GID<?> entityValue, ConversionContext context) {
        return entityValue != null ? entityValue.asUUID() : null;
    }

    @Override
    public GID<?> convertToEntityValue(UUID persistedValue, ConversionContext context) {
        return persistedValue != null ? GID.fromUuid(persistedValue) : null;
    }
}
