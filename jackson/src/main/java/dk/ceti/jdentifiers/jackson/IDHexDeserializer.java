package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import dk.ceti.jdentifiers.id.ID;

import java.io.IOException;
import java.io.Serial;

public class IDHexDeserializer extends StdScalarDeserializer<ID<?>> {
    @Serial
    private static final long serialVersionUID = 1L;

    public IDHexDeserializer() {
        super(ID.class);
    }

    @Override
    public ID<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        try {
            return ID.fromString(text);
        } catch (IllegalArgumentException e) {
            return (ID<?>) ctxt.handleWeirdStringValue(ID.class, text,
                "Invalid ID format: %s", e.getMessage()
            );
        }
    }
}
