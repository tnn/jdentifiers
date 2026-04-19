package dk.ceti.jdentifiers.micronaut.data;

import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field as a {@link dk.ceti.jdentifiers.id.LID} column stored as INTEGER.
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD})
@MappedProperty(converter = LIDAttributeConverter.class)
@TypeDef(type = DataType.INTEGER)
public @interface MappedLid {
}
