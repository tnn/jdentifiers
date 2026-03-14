package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import dk.ceti.jdentifiers.id.LID;

import java.io.IOException;
import java.io.Serial;

public class LIDHexDeserializer extends StdScalarDeserializer<LID<?>> {
    @Serial
    private static final long serialVersionUID = 1L;

    public LIDHexDeserializer() {
        super(LID.class);
    }

    @Override
    public LID<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        try {
            return LID.fromString(text);
        } catch (IllegalArgumentException e) {
            return (LID<?>) ctxt.handleWeirdStringValue(LID.class, text,
                "Invalid LID format: %s", e.getMessage());
        }
    }
}
