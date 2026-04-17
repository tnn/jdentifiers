package dk.ceti.jdentifiers.id;

import java.time.Instant;
import java.util.UUID;

/**
 * Debug utility for analyzing k-sortable identifiers and producing
 * human-readable multi-line output of their bit layout.
 */
public final class KSortableIDDebugger {

    private KSortableIDDebugger() {}

    /**
     * Analyzes a k-sortable 64-bit ID using the given generator's configuration.
     *
     * @param id        the identifier to analyze
     * @param generator the generator whose configuration to use
     * @return human-readable debug string
     */
    public static String debug(ID<?> id, KSortableIDGenerator generator) {
        return debugId(id, generator.nodeBits(), KSortableIDGenerator.DEFAULT_EPOCH_MS);
    }

    /**
     * Analyzes a k-sortable 64-bit ID with explicit parameters.
     *
     * @param id        the identifier to analyze
     * @param nodeBits  number of bits allocated to the node ID (0–22)
     * @param epochMs   custom epoch in Unix milliseconds
     * @return human-readable debug string
     */
    public static String debugId(ID<?> id, int nodeBits, long epochMs) {
        long bits = id.asLong();
        int counterBits = KSortableIDGenerator.ID_PAYLOAD_BITS - nodeBits;

        long timestamp = bits >>> KSortableIDGenerator.ID_PAYLOAD_BITS;
        int node = (nodeBits > 0)
                ? (int) ((bits >>> counterBits) & ((1L << nodeBits) - 1))
                : 0;
        long counter = bits & ((1L << counterBits) - 1);

        long unixMs = timestamp + epochMs;
        Instant instant = Instant.ofEpochMilli(unixMs);

        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(id).append('\n');
        sb.append("├── Timestamp: ").append(KSortableIDGenerator.ID_TIMESTAMP_BITS)
          .append(" bits = ").append(timestamp).append(" ms (").append(instant).append(")\n");
        if (nodeBits > 0) {
            sb.append("├── Node:      ").append(nodeBits)
              .append(" bits = ").append(node).append('\n');
        }
        sb.append("└── Counter:   ").append(counterBits)
          .append(" bits = ").append(counter);
        return sb.toString();
    }

    /**
     * Analyzes a k-sortable 32-bit LID using the given generator's configuration.
     *
     * @param lid       the local identifier to analyze
     * @param generator the generator whose configuration to use
     * @return human-readable debug string
     */
    public static String debug(LID<?> lid, KSortableIDGenerator generator) {
        return debugLid(lid, generator.lidEpochMs());
    }

    /**
     * Analyzes a k-sortable 32-bit LID with explicit epoch.
     *
     * @param lid      the local identifier to analyze
     * @param epochMs  custom epoch in Unix milliseconds
     * @return human-readable debug string
     */
    public static String debugLid(LID<?> lid, long epochMs) {
        int bits = lid.toInteger();

        long hoursSinceEpoch = Integer.toUnsignedLong(bits) >>> KSortableIDGenerator.LID_COUNTER_BITS;
        int counter = bits & KSortableIDGenerator.LID_COUNTER_MAX;

        long unixMs = (hoursSinceEpoch * KSortableIDGenerator.HOUR_MS) + epochMs;
        Instant instant = Instant.ofEpochMilli(unixMs);

        var sb = new StringBuilder();
        sb.append("LID: ").append(lid).append('\n');
        sb.append("├── Timestamp: ").append(KSortableIDGenerator.LID_TIMESTAMP_BITS)
          .append(" bits = ").append(hoursSinceEpoch).append(" hours (").append(instant).append(")\n");
        sb.append("└── Counter:   ").append(KSortableIDGenerator.LID_COUNTER_BITS)
          .append(" bits = ").append(counter);
        return sb.toString();
    }

    /**
     * Analyzes a UUIDv7 global identifier.
     *
     * @param gid the global identifier to analyze
     * @return human-readable debug string
     */
    public static String debug(GID<?> gid) {
        UUID uuid = gid.asUUID();
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        long unixMs = msb >>> 16;
        int version = (int) ((msb >>> 12) & 0xF);
        int randA = (int) (msb & 0xFFF);
        int variant = (int) ((lsb >>> 62) & 0x3);
        long randB = lsb & 0x3FFFFFFFFFFFFFFFL;

        Instant instant = Instant.ofEpochMilli(unixMs);

        StringBuilder sb = new StringBuilder();
        sb.append("GID: ").append(gid).append('\n');
        sb.append("├── Timestamp: 48 bits = ").append(unixMs)
          .append(" ms (").append(instant).append(")\n");
        sb.append("├── Version:   4 bits  = ").append(version).append('\n');
        sb.append("├── rand_a:    12 bits = ").append(randA).append(" (counter)\n");
        sb.append("├── Variant:   2 bits  = ").append(variant).append('\n');
        sb.append("└── rand_b:    62 bits = 0x").append(Long.toHexString(randB));
        return sb.toString();
    }
}
