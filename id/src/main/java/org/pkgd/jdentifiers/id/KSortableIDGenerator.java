package org.pkgd.jdentifiers.id;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class KSortableIDGenerator implements IDGenerator {
    static final long customEpoc = ZonedDateTime
            .of(2020, 01, 01, 00, 00, 00, 0, ZoneId.of("UTC"))
            .truncatedTo(ChronoUnit.MILLIS)
            .toInstant()
            .toEpochMilli();
    private static final long randomBits = 24;
    private static final long timeLeftShift = randomBits;
    private static final long randComponentMask = ~(-1L << randomBits);

    private final Clock clock;
    private final SecureRandom random;

    private long lastTimestamp;

    public KSortableIDGenerator() {
        this(Clock.systemUTC());
    }

    public KSortableIDGenerator(Clock clock) {
        this.clock = clock;
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        lastTimestamp = -1L;
    }

    @Override
    public <T extends IDAble> LID<T> localIdentifier() {
        return null;
    }

    /**
     * 64-bit identifier, first 40-bits is truncated time, last 24-bits are random.
     *
     * @return
     * @param <T>
     */
    @Override
    public <T extends IDAble> ID<T> identifier() {
        final var epoch = clock.millis();
        if (lastTimestamp > epoch) {
            // Smartness: wait up to one second?
            throw new IllegalStateException("System clock is going back in time!");
        }
        var randComponent = (long) random.nextInt();
        lastTimestamp = epoch;
        return ID.fromLong(
                ((epoch - customEpoc) << timeLeftShift) | (randComponent & randComponentMask)
        );
    }

    @Override
    public <T extends IDAble> GID<T> globalIdentifier() {
        return null;
    }
}
