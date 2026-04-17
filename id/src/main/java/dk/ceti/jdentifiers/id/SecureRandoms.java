package dk.ceti.jdentifiers.id;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Shared {@link SecureRandom} factory used by both {@link KSortableIDGenerator}
 * and {@link RandomIDGenerator}.
 *
 * <p>Prefers DRBG (deterministic random bit generator, available on JDK 9+) for its
 * periodic reseeding, then falls back to SHA1PRNG for predictable non-blocking
 * latency, and finally to the platform default.
 */
final class SecureRandoms {

    private static final String[] ALGORITHMS = {"DRBG", "SHA1PRNG"};

    private SecureRandoms() {
    }

    static SecureRandom create() {
        for (String algorithm : ALGORITHMS) {
            try {
                return SecureRandom.getInstance(algorithm);
            } catch (NoSuchAlgorithmException ignored) {
                // Fall through to next algorithm or platform default
            }
        }
        return new SecureRandom();
    }
}
