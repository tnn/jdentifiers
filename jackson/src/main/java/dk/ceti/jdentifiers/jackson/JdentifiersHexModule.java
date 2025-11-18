package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.pkgd.jdentifiers.id.GID;
import org.pkgd.jdentifiers.id.ID;

import java.io.Serial;

public class JdentifiersHexModule extends SimpleModule {
    @Serial
    private static final long serialVersionUID = 1L;

    public JdentifiersHexModule() {
        super(JdentifiersHexModule.class.getSimpleName(), PackageVersion.VERSION);
        addSerializer((Class) ID.class, new IDHexSerializer());
        addSerializer((Class) GID.class, new GIDHexSerializer());

        addDeserializer((Class) ID.class, new IDHexDeserializer());
        addDeserializer((Class) GID.class, new GIDHexDeserializer());

    }
}
