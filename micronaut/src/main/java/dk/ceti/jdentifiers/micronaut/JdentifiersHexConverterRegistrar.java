package dk.ceti.jdentifiers.micronaut;

import dk.ceti.jdentifiers.id.GID;
import dk.ceti.jdentifiers.id.ID;
import dk.ceti.jdentifiers.id.LID;
import io.micronaut.core.convert.MutableConversionService;
import io.micronaut.core.convert.TypeConverterRegistrar;

import java.util.Optional;
import java.util.UUID;

/**
 * Registers hex-string type converters for {@link ID}, {@link LID} and
 * {@link GID} with Micronaut's conversion service.
 *
 * <p>Handles HTTP parameter binding, configuration value injection,
 * and general type conversion throughout the framework. String
 * representations use lowercase hex (ID, LID) or UUID format (GID),
 * consistent with the Jackson module.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class JdentifiersHexConverterRegistrar implements TypeConverterRegistrar {

    @Override
    public void register(MutableConversionService cs) {
        // ID — 64-bit, hex string
        cs.addConverter(Long.class, ID.class, l -> ID.fromLong(l));
        cs.addConverter(ID.class, Long.class, id -> id.asLong());
        cs.addConverter(ID.class, String.class, id -> id.toString());
        cs.addConverter(String.class, ID.class,
            (s, target, ctx) -> (Optional) ID.parse(s));

        // LID — 32-bit, hex string
        cs.addConverter(Integer.class, LID.class, i -> LID.fromInt(i));
        cs.addConverter(LID.class, Integer.class, lid -> lid.asInt());
        cs.addConverter(LID.class, String.class, lid -> lid.toString());
        cs.addConverter(String.class, LID.class,
            (s, target, ctx) -> (Optional) LID.parse(s));

        // GID — 128-bit, UUID string
        cs.addConverter(UUID.class, GID.class, u -> GID.fromUuid(u));
        cs.addConverter(GID.class, UUID.class, gid -> gid.asUUID());
        cs.addConverter(GID.class, String.class, gid -> gid.toString());
        cs.addConverter(String.class, GID.class,
            (s, target, ctx) -> (Optional) GID.parse(s));
    }
}
