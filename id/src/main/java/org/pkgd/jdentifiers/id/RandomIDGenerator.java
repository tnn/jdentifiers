package org.pkgd.jdentifiers.id;

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
                // The default algorithm in Hotspot 17 may be use NativePRNG
                // which can block in case there isn't enough entropy available.
                // The random numbers we generate here has too little entropy to be
                // cryptographically secure anyway, thus we choose performance over security.
                // TODO: Check if DRBG performance is better
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

    @Override
    public <T extends IDAble> GID<T> globalIdentifier() {
        return GID.fromUuid(UUID.randomUUID());
    }
}
