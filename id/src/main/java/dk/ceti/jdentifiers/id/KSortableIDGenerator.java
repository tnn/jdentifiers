package dk.ceti.jdentifiers.id;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.IntSupplier;

/**
 * K-sortable identifier generator producing time-sorted IDs.
 * <p>
 * Bit layouts:
 * <ul>
 *   <li><b>GID (128-bit)</b> — UUIDv7 per RFC 9562: 48-bit Unix ms timestamp,
 *       4-bit version ({@code 0111}), 12-bit monotonic counter, 2-bit variant ({@code 10}),
 *       62-bit random.</li>
 *   <li><b>ID (64-bit)</b> — TSID-style: 42-bit ms timestamp (custom epoch 2020-01-01),
 *       configurable node + counter split over the remaining 22 bits.</li>
 *   <li><b>LID (32-bit)</b> — Hour-precision: 20-bit hour timestamp (configurable epoch,
 *       default 2020-01-01), 12-bit monotonic counter.</li>
 * </ul>
 * <p>
 * Thread-safe: each generation method is guarded by a dedicated lock, so a
 * spin-wait in one method cannot block the others.
 *
 * <h2>Per-entity-type isolation</h2>
 * A single generator instance shares counter state across all phantom types —
 * {@code ID<User>} and {@code ID<Organization>} draw from the same counter.
 * This guarantees that all IDs produced by one generator have distinct numeric
 * values, even across entity types (useful for untyped storage and logging).
 * <p>
 * If per-entity-type throughput isolation is needed (independent counter spaces),
 * create separate generator instances from a shared builder:
 * <pre>{@code
 * KSortableIDGenerator.Builder base = KSortableIDGenerator.builder()
 *     .nodeBits(10).nodeId(42);
 * KSortableIDGenerator userGen = base.copy().build();
 * KSortableIDGenerator orgGen  = base.copy().build();
 * }</pre>
 * Note: IDs from separate generators may have identical numeric values for the
 * same timestamp. Use this pattern only when entity types are stored in
 * type-discriminated columns or tables.
 *
 * @see RandomIDGenerator
 */
public class KSortableIDGenerator implements IDGenerator {

    /**
     * Custom epoch: 2020-01-01T00:00:00Z in Unix milliseconds.
     * <p>
     * Used unconditionally for 64-bit ID timestamps. Unlike the LID epoch
     * (configurable via {@link Builder#lidEpoch(Instant)}), the ID epoch is
     * fixed because changing it would break sort order and timestamp extraction
     * for any previously generated IDs. LID epoch is configurable because LIDs
     * are scoped within composite keys, so different deployments can choose
     * independent epochs without cross-system ordering concerns.
     */
    static final long DEFAULT_EPOCH_MS = 1_577_836_800_000L;

    static final long HOUR_MS = 3_600_000L;
    static final int ID_PAYLOAD_BITS = 22;
    static final int ID_TIMESTAMP_BITS = 42;
    static final int LID_TIMESTAMP_BITS = 20;
    static final int LID_COUNTER_BITS = 12;
    static final int LID_COUNTER_MAX = (1 << LID_COUNTER_BITS) - 1; // 4095
    static final int GID_COUNTER_MAX = 0xFFF; // 4095
    static final int GID_TIMESTAMP_BITS = 48;

    private static final int GID_COUNTER_INIT_BOUND = 1 << 8; // 256
    private static final long MAX_CLOCK_REGRESSION_MS = 1_000L;

    /** Maximum time to spin-wait for clock advancement before throwing. */
    static final long MAX_SPIN_NANOS = 2_000_000_000L; // 2 seconds

    private final Clock clock;
    private final int nodeBits;
    private final int counterBits;
    private final int counterMax;
    private final int nodeId;
    private final long lidEpochMs;
    private final long maxSpinNanos;
    private final LidOverflowPolicy lidOverflowPolicy;
    private final SecureRandom random;

    // Separate locks per ID type — prevents cross-type head-of-line blocking
    private final Object idLock = new Object();
    private final Object gidLock = new Object();
    private final Object lidLock = new Object();

    // ID (64-bit) mutable state — guarded by idLock
    private long lastIdTimestamp = -1;
    private int idCounter;

    // LID (32-bit) mutable state — guarded by lidLock
    private long lastLidHour = -1;
    private int lidCounter;

    // GID (128-bit) mutable state — guarded by gidLock
    private long lastGidTimestamp = -1;
    private int gidCounter;

    private KSortableIDGenerator(Clock clock, int nodeBits, int nodeId,
                                  long lidEpochMs, long maxSpinNanos,
                                  LidOverflowPolicy lidOverflowPolicy) {
        this.clock = clock;
        this.nodeBits = nodeBits;
        this.counterBits = ID_PAYLOAD_BITS - nodeBits;
        this.counterMax = (1 << this.counterBits) - 1;
        this.nodeId = nodeId;
        this.lidEpochMs = lidEpochMs;
        this.maxSpinNanos = maxSpinNanos;
        this.lidOverflowPolicy = lidOverflowPolicy;

        this.random = SecureRandoms.create();
    }

    /**
     * Creates a single-node generator with default epoch (2020-01-01) and no node bits.
     * <p>
     * Equivalent to {@code KSortableIDGenerator.builder().build()}.
     */
    public KSortableIDGenerator() {
        this(Clock.systemUTC(), 0, 0, DEFAULT_EPOCH_MS, MAX_SPIN_NANOS, LidOverflowPolicy.WRAP);
    }

    /**
     * Returns a new {@link Builder} for configuring a generator.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private static void checkIdTimestamp(long timestamp) {
        if (timestamp >>> ID_TIMESTAMP_BITS != 0) {
            throw new IllegalStateException("42-bit timestamp overflow");
        }
    }

    private static void checkGidTimestamp(long now) {
        if (now >>> GID_TIMESTAMP_BITS != 0) {
            throw new IllegalStateException("48-bit timestamp overflow");
        }
    }

    /**
     * Generates a k-sortable 64-bit identifier.
     * <p>
     * Layout: {@code [42-bit ms timestamp | node bits | counter bits]} where
     * node + counter = 22 bits (configurable split via {@link Builder#nodeBits(int)}).
     * <p>
     * On counter overflow, blocks until the next millisecond tick.
     * On clock regression ≤1s, spin-waits; &gt;1s throws {@link IllegalStateException}.
     */
    @Override
    public <T extends IDAble> ID<T> identifier() {
        synchronized (idLock) {
            long now = clock.millis();
            long timestamp = now - DEFAULT_EPOCH_MS;

            if (timestamp < 0) {
                throw new IllegalStateException("Clock is before epoch 2020-01-01T00:00:00Z");
            }
            checkIdTimestamp(timestamp);

            if (timestamp == lastIdTimestamp) {
                if (idCounter <= counterMax) {
                    idCounter++;
                }
                if (idCounter > counterMax) {
                    long deadline = System.nanoTime() + maxSpinNanos;
                    while ((now = clock.millis()) - DEFAULT_EPOCH_MS == lastIdTimestamp) {
                        Thread.onSpinWait();
                        if (System.nanoTime() - deadline >= 0) {
                            throw new IllegalStateException(
                                "Clock did not advance within timeout");
                        }
                    }
                    timestamp = now - DEFAULT_EPOCH_MS;
                    checkIdTimestamp(timestamp);
                    idCounter = 0;
                }
            } else if (timestamp > lastIdTimestamp) {
                idCounter = 0;
            } else {
                long drift = lastIdTimestamp - timestamp;
                if (drift <= MAX_CLOCK_REGRESSION_MS) {
                    long deadline = System.nanoTime() + maxSpinNanos;
                    while ((now = clock.millis()) - DEFAULT_EPOCH_MS <= lastIdTimestamp) {
                        Thread.onSpinWait();
                        if (System.nanoTime() - deadline >= 0) {
                            throw new IllegalStateException(
                                "Clock did not advance within timeout");
                        }
                    }
                    timestamp = now - DEFAULT_EPOCH_MS;
                    checkIdTimestamp(timestamp);
                    idCounter = 0;
                } else {
                    throw new IllegalStateException(
                        "Clock moved backwards by " + drift + "ms (exceeds 1s tolerance)");
                }
            }

            lastIdTimestamp = timestamp;

            long bits = (timestamp << ID_PAYLOAD_BITS)
                      | ((long) nodeId << counterBits)
                      | idCounter;
            return ID.fromLong(bits);
        }
    }

    /**
     * Generates a UUIDv7 global identifier per RFC 9562.
     * <p>
     * Layout:
     * <pre>
     * MSB: [48-bit Unix ms timestamp | 4-bit version (0111) | 12-bit counter]
     * LSB: [2-bit variant (10) | 62-bit random]
     * </pre>
     * <p>
     * The 12-bit counter resets to a small random offset each new millisecond tick
     * (RFC 9562 Method 1). On counter overflow, blocks until the next tick.
     */
    @Override
    public <T extends IDAble> GID<T> globalIdentifier() {
        long randomLsb = (random.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        synchronized (gidLock) {
            long now = clock.millis();

            checkGidTimestamp(now);

            if (now == lastGidTimestamp) {
                if (gidCounter <= GID_COUNTER_MAX) {
                    gidCounter++;
                }
                if (gidCounter > GID_COUNTER_MAX) {
                    long deadline = System.nanoTime() + maxSpinNanos;
                    while ((now = clock.millis()) == lastGidTimestamp) {
                        Thread.onSpinWait();
                        if (System.nanoTime() - deadline >= 0) {
                            throw new IllegalStateException(
                                "Clock did not advance within timeout");
                        }
                    }
                    checkGidTimestamp(now);
                    gidCounter = random.nextInt(GID_COUNTER_INIT_BOUND);
                }
            } else if (now > lastGidTimestamp) {
                gidCounter = random.nextInt(GID_COUNTER_INIT_BOUND);
            } else {
                long drift = lastGidTimestamp - now;
                if (drift <= MAX_CLOCK_REGRESSION_MS) {
                    long deadline = System.nanoTime() + maxSpinNanos;
                    while ((now = clock.millis()) <= lastGidTimestamp) {
                        Thread.onSpinWait();
                        if (System.nanoTime() - deadline >= 0) {
                            throw new IllegalStateException(
                                "Clock did not advance within timeout");
                        }
                    }
                    checkGidTimestamp(now);
                    gidCounter = random.nextInt(GID_COUNTER_INIT_BOUND);
                } else {
                    throw new IllegalStateException(
                        "Clock moved backwards by " + drift + "ms (exceeds 1s tolerance)");
                }
            }

            lastGidTimestamp = now;

            long msb = (now << 16) | (0x7L << 12) | (gidCounter & 0xFFF);

            return GID.fromUuid(new UUID(msb, randomLsb));
        }
    }

    /**
     * Generates a k-sortable 32-bit local identifier with hour precision.
     * <p>
     * Layout: {@code [20-bit hour timestamp | 12-bit counter]}.
     * <p>
     * The 12-bit counter supports up to 4,096 IDs per hour. When the counter
     * is exhausted, the behavior depends on the configured
     * {@link LidOverflowPolicy}:
     * <ul>
     *   <li>{@link LidOverflowPolicy#WRAP} (default) — resets the counter to
     *       zero, accepting that duplicate values may be produced within the
     *       same hour. Safe when the LID is part of a composite primary key
     *       whose uniqueness is enforced by the database.</li>
     *   <li>{@link LidOverflowPolicy#THROW} — throws
     *       {@link IllegalStateException}. Use when there is no external
     *       uniqueness constraint to catch duplicates.</li>
     * </ul>
     * For sustained high throughput beyond 4,096/hour, consider separate
     * generator instances per scope (via {@link Builder#copy()}) or
     * {@link RandomIDGenerator#localIdentifier()} for non-sortable IDs.
     */
    @Override
    public <T extends IDAble> LID<T> localIdentifier() {
        synchronized (lidLock) {
            long now = clock.millis();
            long hoursSinceEpoch = (now - lidEpochMs) / HOUR_MS;

            if (hoursSinceEpoch < 0) {
                throw new IllegalStateException("Clock is before LID epoch");
            }
            if (hoursSinceEpoch >>> LID_TIMESTAMP_BITS != 0) {
                throw new IllegalStateException("20-bit hour timestamp overflow");
            }

            if (hoursSinceEpoch == lastLidHour) {
                if (lidCounter >= LID_COUNTER_MAX) {
                    if (lidOverflowPolicy == LidOverflowPolicy.THROW) {
                        throw new IllegalStateException(
                            "LID counter overflow: more than 4096 IDs generated in the current hour. " +
                            "Consider LidOverflowPolicy.WRAP or RandomIDGenerator.");
                    }
                    lidCounter = 0;
                } else {
                    lidCounter++;
                }
            } else if (hoursSinceEpoch > lastLidHour) {
                lidCounter = 0;
            } else {
                throw new IllegalStateException("Clock moved backwards across hour boundary");
            }

            lastLidHour = hoursSinceEpoch;

            int bits = (int) ((hoursSinceEpoch << LID_COUNTER_BITS) | lidCounter);
            return LID.fromInteger(bits);
        }
    }

    /**
     * Returns the number of node bits configured for ID generation.
     *
     * @return node bits (0 to 21)
     */
    public int nodeBits() {
        return nodeBits;
    }

    /**
     * Returns the number of counter bits for ID generation ({@code 22 - nodeBits}).
     *
     * @return counter bits
     */
    public int counterBits() {
        return counterBits;
    }

    /**
     * Returns the configured node ID.
     *
     * @return the node ID
     */
    public int nodeId() {
        return nodeId;
    }

    /**
     * Returns the LID epoch in Unix milliseconds.
     *
     * @return epoch millis
     */
    public long lidEpochMs() {
        return lidEpochMs;
    }

    /**
     * Returns the LID overflow policy.
     *
     * @return the overflow policy
     */
    public LidOverflowPolicy lidOverflowPolicy() {
        return lidOverflowPolicy;
    }

    /**
     * Builder for {@link KSortableIDGenerator}.
     */
    public static final class Builder {
        private Clock clock = Clock.systemUTC();
        private int nodeBits;
        private Integer nodeId;
        private IntSupplier nodeIdFactory;
        private Instant lidEpoch;
        private long maxSpinNanos = MAX_SPIN_NANOS;
        private LidOverflowPolicy lidOverflowPolicy = LidOverflowPolicy.WRAP;

        private Builder() {}

        /**
         * Clock source for timestamp generation. Defaults to {@link Clock#systemUTC()}.
         *
         * @param clock the clock to use
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * Number of bits allocated to the node ID within the 22-bit ID payload.
         * The remaining {@code 22 - nodeBits} bits are used for the counter.
         * Defaults to 0 (single-node, full 22-bit counter).
         *
         * @param nodeBits the number of node bits (0 to 21)
         * @return this builder
         */
        public Builder nodeBits(int nodeBits) {
            this.nodeBits = nodeBits;
            return this;
        }

        /**
         * Static node ID. Must fit within {@link #nodeBits(int)} bits.
         *
         * @param nodeId the node identifier
         * @return this builder
         */
        public Builder nodeId(int nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        /**
         * Factory for dynamic node ID resolution, called exactly once at {@link #build()} time.
         * The returned value is captured and used for the lifetime of the generator.
         * Cannot be combined with {@link #nodeId(int)}.
         *
         * @param factory supplier that returns the node ID
         * @return this builder
         */
        public Builder nodeIdFactory(IntSupplier factory) {
            this.nodeIdFactory = Objects.requireNonNull(factory, "factory");
            return this;
        }

        /**
         * Custom epoch for LID hour-precision timestamps.
         * Defaults to 2020-01-01T00:00:00Z.
         *
         * @param epoch the epoch instant
         * @return this builder
         */
        public Builder lidEpoch(Instant epoch) {
            this.lidEpoch = Objects.requireNonNull(epoch, "epoch");
            return this;
        }

        /**
         * Controls what happens when the LID 12-bit counter is exhausted within
         * a single hour. Defaults to {@link LidOverflowPolicy#WRAP}.
         *
         * @param policy the overflow policy
         * @return this builder
         */
        public Builder lidOverflowPolicy(LidOverflowPolicy policy) {
            this.lidOverflowPolicy = Objects.requireNonNull(policy, "lidOverflowPolicy");
            return this;
        }

        /**
         * Returns a new Builder pre-populated with this builder's configuration.
         * Useful for creating multiple identically-configured generator instances
         * when per-entity-type counter isolation is desired.
         *
         * @return a new builder with the same configuration
         */
        public Builder copy() {
            Builder b = new Builder();
            b.clock = this.clock;
            b.nodeBits = this.nodeBits;
            b.nodeId = this.nodeId;
            b.nodeIdFactory = this.nodeIdFactory;
            b.lidEpoch = this.lidEpoch;
            b.maxSpinNanos = this.maxSpinNanos;
            b.lidOverflowPolicy = this.lidOverflowPolicy;
            return b;
        }

        // Package-private — visible for testing
        Builder maxSpinNanos(long maxSpinNanos) {
            if (maxSpinNanos <= 0) {
                throw new IllegalArgumentException("maxSpinNanos must be positive, got " + maxSpinNanos);
            }
            this.maxSpinNanos = maxSpinNanos;
            return this;
        }

        /**
         * Creates the generator.
         *
         * @return a new generator
         * @throws IllegalArgumentException if the configuration is invalid
         */
        public KSortableIDGenerator build() {
            if (nodeBits < 0 || nodeBits >= ID_PAYLOAD_BITS) {
                throw new IllegalArgumentException(
                    "nodeBits must be in [0, " + (ID_PAYLOAD_BITS - 1)
                    + "] (at least 1 counter bit required), got " + nodeBits);
            }

            if (nodeId != null && nodeIdFactory != null) {
                throw new IllegalArgumentException("Cannot set both nodeId and nodeIdFactory; use one or the other");
            }

            int resolvedNodeId = getResolvedNodeId();
            long resolvedLidEpochMs = (lidEpoch != null) ? lidEpoch.toEpochMilli() : DEFAULT_EPOCH_MS;

            return new KSortableIDGenerator(clock, nodeBits, resolvedNodeId,
                resolvedLidEpochMs, maxSpinNanos, lidOverflowPolicy);
        }

        private int getResolvedNodeId() {
            int resolvedNodeId;
            if (nodeIdFactory != null) {
                resolvedNodeId = nodeIdFactory.getAsInt();
            } else {
                resolvedNodeId = Objects.requireNonNullElse(nodeId, 0);
            }

            if (nodeBits == 0) {
                if (resolvedNodeId != 0) {
                    throw new IllegalArgumentException(
                        "nodeId must be 0 when nodeBits is 0, got " + resolvedNodeId);
                }
            } else {
                int maxNodeId = (1 << nodeBits) - 1;
                if (resolvedNodeId < 0 || resolvedNodeId > maxNodeId) {
                    throw new IllegalArgumentException(
                        "nodeId must be in [0, " + maxNodeId + "] for "
                        + nodeBits + " node bits, got " + resolvedNodeId);
                }
            }
            return resolvedNodeId;
        }
    }
}
