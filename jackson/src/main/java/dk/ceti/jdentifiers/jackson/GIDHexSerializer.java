package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import dk.ceti.jdentifiers.id.GID;

import java.io.IOException;
import java.io.Serial;

public class GIDHexSerializer extends StdScalarSerializer<GID<?>> {
    @Serial
    private static final long serialVersionUID = 1L;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GIDHexSerializer() {
        super((Class) GID.class);
    }

    @Override
    public void serialize(GID<?> value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
        gen.writeString(value.toString());
    }
}
