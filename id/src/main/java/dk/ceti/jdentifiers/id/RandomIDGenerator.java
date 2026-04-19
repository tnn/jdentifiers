package dk.ceti.jdentifiers.id;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * ID generator that produces uniformly random (non-sortable) identifiers.
 *
 * @see KSortableIDGenerator
 */
public class RandomIDGenerator implements IDGenerator {

    private static final SecureRandom numberGenerator = SecureRandoms.create();

    /**
     * Creates a new random ID generator.
     */
    public RandomIDGenerator() {
    }


    @Override
    public <T extends IDAble> LID<T> localIdentifier() {
        return LID.fromInt(numberGenerator.nextInt());
    }

    @Override
    public <T extends IDAble> ID<T> identifier() {
        return ID.fromLong(numberGenerator.nextLong());
    }

    /**
     * Generates a globally unique 128-bit identifier using {@link UUID#randomUUID()}.
     *
     * <p>Note: this delegates to the JDK's {@code UUID.randomUUID()}, which uses
     * the JDK's default {@link java.security.SecureRandom} — not the SHA1PRNG
     * instance used by {@link #identifier()} and {@link #localIdentifier()}.
     * This is intentional: {@code UUID.randomUUID()} is the canonical way to
     * generate UUIDv4 values and ensures correct variant/version bits.
     */
    @Override
    public <T extends IDAble> GID<T> globalIdentifier() {
        return GID.fromUuid(UUID.randomUUID());
    }
}
