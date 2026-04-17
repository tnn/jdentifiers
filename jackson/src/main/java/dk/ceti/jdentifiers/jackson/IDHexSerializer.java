package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import dk.ceti.jdentifiers.id.ID;

import java.io.IOException;
import java.io.Serial;

public class IDHexSerializer extends StdScalarSerializer<ID<?>> {
    @Serial
    private static final long serialVersionUID = 1L;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public IDHexSerializer() {
        super((Class) ID.class);
    }

    @Override
    public void serialize(ID<?> value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeString(value.toString());
    }
}
