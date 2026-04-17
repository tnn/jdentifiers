package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import dk.ceti.jdentifiers.id.LID;

import java.io.IOException;
import java.io.Serial;

public class LIDHexSerializer extends StdScalarSerializer<LID<?>> {
    @Serial
    private static final long serialVersionUID = 1L;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public LIDHexSerializer() {
        super((Class) LID.class);
    }

    @Override
    public void serialize(LID<?> value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeString(value.toString());
    }
}
