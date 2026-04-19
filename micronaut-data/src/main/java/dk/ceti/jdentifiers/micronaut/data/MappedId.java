package dk.ceti.jdentifiers.micronaut.data;

import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field as an {@link dk.ceti.jdentifiers.id.ID} column stored as BIGINT.
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD})
@MappedProperty(converter = IDAttributeConverter.class)
@TypeDef(type = DataType.LONG)
public @interface MappedId {
}
