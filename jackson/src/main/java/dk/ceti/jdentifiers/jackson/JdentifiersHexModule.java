package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dk.ceti.jdentifiers.id.GID;
import dk.ceti.jdentifiers.id.ID;
import dk.ceti.jdentifiers.id.LID;

import java.io.Serial;

public class JdentifiersHexModule extends SimpleModule {
    @Serial
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes")
    public JdentifiersHexModule() {
        super(JdentifiersHexModule.class.getSimpleName(), PackageVersion.VERSION);
        addSerializer((Class) ID.class, new IDHexSerializer());
        addSerializer((Class) GID.class, new GIDHexSerializer());
        addSerializer((Class) LID.class, new LIDHexSerializer());

        addDeserializer((Class) ID.class, new IDHexDeserializer());
        addDeserializer((Class) GID.class, new GIDHexDeserializer());
        addDeserializer((Class) LID.class, new LIDHexDeserializer());
    }
}
