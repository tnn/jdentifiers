package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import dk.ceti.jdentifiers.id.GID;

import java.io.IOException;
import java.io.Serial;

public class GIDHexDeserializer extends StdScalarDeserializer<GID<?>> {
    @Serial
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes")
    public GIDHexDeserializer() {
        super((Class) GID.class);
    }

    @Override
    public GID<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String text = p.getText();
        try {
            return GID.fromString(text);
        } catch (IllegalArgumentException e) {
            // Use Jackson's helper to create a JsonMappingException
            // This automatically attaches line/column numbers and the field path
            return (GID) ctxt.handleWeirdStringValue(GID.class, text,
                "Invalid ID format: %s", e.getMessage());
        }
    }
}
