package org.pkgd.jdentifiers.id;

import java.time.Clock;

public class KSortableIDGenerator implements IDGenerator {

    private final Clock clock;

    public KSortableIDGenerator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public <T extends IDAble> LID<T> localIdentifier() {
        return null;
    }

    @Override
    public <T extends IDAble> ID<T> identifier() {
        var timestamp = clock.millis();
        // var sequence number
        //
        return ID.fromLong(timestamp);
    }

    @Override
    public <T extends IDAble> GID<T> globalIdentifier() {
        return null;
    }
}
