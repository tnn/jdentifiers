package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import org.pkgd.jdentifiers.id.ID;

public class IDDeserializer implements ContextualDeserializer<ID<?>> {
  private static final long serialVersionUID = 1L;

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext, BeanProperty beanProperty) throws JsonMappingException {
    return null;
  }
}
