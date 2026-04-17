package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RandomIDGeneratorTest {

    private final RandomIDGenerator generator = new RandomIDGenerator();

    @Test
    void globalIdentifier_hasUuidV4Version() {
        for (int i = 0; i < 100; i++) {
            GID<TestEntity> gid = generator.globalIdentifier();
            UUID uuid = gid.asUUID();
            assertEquals(4, uuid.version(),
                "UUID version must be 4, got " + uuid.version() + " for " + uuid
            );
        }
    }

    @Test
    void globalIdentifier_hasRfc4122Variant() {
        for (int i = 0; i < 100; i++) {
            GID<TestEntity> gid = generator.globalIdentifier();
            UUID uuid = gid.asUUID();
            assertEquals(2, uuid.variant(),
                "UUID variant must be 2 (RFC 4122), got " + uuid.variant() + " for " + uuid
            );
        }
    }

    @Test
    void globalIdentifier_versionAndVariantBitsAreCorrect() {
        for (int i = 0; i < 100; i++) {
            GID<TestEntity> gid = generator.globalIdentifier();
            UUID uuid = gid.asUUID();
            long msb = uuid.getMostSignificantBits();
            long lsb = uuid.getLeastSignificantBits();

            // Version nibble (bits 48-51 of MSB) must be 0100
            assertEquals(0x4L, (msb >>> 12) & 0xF,
                "Version nibble mismatch for " + uuid
            );

            // Variant bits (bits 62-63 of LSB) must be 10
            assertEquals(0b10, (lsb >>> 62) & 0b11,
                "Variant bits mismatch for " + uuid
            );
        }
    }

    @Test
    void identifier_producesUniqueValues() {
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            ID<TestEntity> id = generator.identifier();
            seen.add(id.asLong());
        }
        assertEquals(1_000, seen.size());
    }

    @Test
    void localIdentifier_producesUniqueValues() {
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            LID<TestEntity> lid = generator.localIdentifier();
            seen.add(lid.toInteger());
        }
        assertEquals(1_000, seen.size());
    }

    interface TestEntity extends IDAble {
    }
}
