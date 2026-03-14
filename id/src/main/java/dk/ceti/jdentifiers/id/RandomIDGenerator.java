package dk.ceti.jdentifiers.id;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

public class RandomIDGenerator implements IDGenerator {
    /**
     * The random number generator used by this class to create random
     * based IDs. In a holder class to defer initialization until needed.
     */
    static final class Holder {
        static final SecureRandom numberGenerator;

        static {
            SecureRandom numberGeneratorTmp;
            try {
                // The default SecureRandom on Linux/macOS (Hotspot 17+) may use NativePRNG,
                // which reads from /dev/random and can block when the OS entropy pool is
                // exhausted. SHA1PRNG seeds once from the OS entropy pool and then generates
                // output deterministically — it never blocks after initial seeding.
                // We choose SHA1PRNG for predictable latency.
                numberGeneratorTmp = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                numberGeneratorTmp = new SecureRandom();
            }
            numberGenerator = numberGeneratorTmp;
        }

        private Holder() {
        }
    }

    @Override
    public <T extends IDAble> LID<T> localIdentifier() {
        return LID.fromInteger(Holder.numberGenerator.nextInt());
    }

    @Override
    public <T extends IDAble> ID<T> identifier() {
        return ID.fromLong(Holder.numberGenerator.nextLong());
    }

    /**
     * Generates a globally unique 128-bit identifier using {@link UUID#randomUUID()}.
     * <p>
     * Note: this delegates to the JDK's {@code UUID.randomUUID()}, which uses
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
