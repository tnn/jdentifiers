package org.pkgd.jdentifiers.id;

import java.time.Clock;

public class KSortableIDGenerator implements IDGenerator {

    private final Clock clock;
    private final int nodeId;

    public KSortableIDGenerator(Clock clock, int nodeId) {
        this.clock = clock;
        this.nodeId = nodeId;
    }

    @Override
    public <T extends IDAble> LID<T> localIdentifier() {
        return null;
    }

    @Override
    public <T extends IDAble> ID<T> identifier() {
        var bits = clock.millis();
        // Last 16 bits set to nodeId
        bits |= (nodeId >> 16) << 4;
        bits |= nodeId >> 24;
        return ID.fromLong(bits);
    }

    @Override
    public <T extends IDAble> GID<T> globalIdentifier() {
        return null;
    }
}
