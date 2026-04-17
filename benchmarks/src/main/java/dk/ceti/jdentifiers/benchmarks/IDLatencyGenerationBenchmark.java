package dk.ceti.jdentifiers.benchmarks;

import dk.ceti.jdentifiers.id.GID;
import dk.ceti.jdentifiers.id.ID;
import dk.ceti.jdentifiers.id.IDAble;
import dk.ceti.jdentifiers.id.IDGenerator;
import dk.ceti.jdentifiers.id.KSortableIDGenerator;
import dk.ceti.jdentifiers.id.LID;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Latency benchmark for ID generation at constant request rates.
 * <p>
 * Warmup runs at a high rate for 10 seconds to trigger JIT compilation and reach
 * steady state. After a 1-second pause, measurement runs at a lower rate for 1 second.
 * <p>
 * Rates:
 * <ul>
 *   <li>ID/GID (64-bit, 128-bit): warmup 10,000 ops/s, measurement 1,000 ops/s</li>
 *   <li>LID (32-bit): warmup 200 ops/s, measurement 50 ops/s (capped by 4,096/hour k-sortable counter)</li>
 * </ul>
 * <p>
 * JMH {@link Mode#SampleTime} records per-invocation latency including the pacing sleep,
 * so the p50 reflects the target interval while p99+ percentiles reveal tail latency
 * caused by synchronization, counter overflow blocking, or clock reads.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 1, time = 1)
@Fork(value = 1)
@Threads(1)
public class IDLatencyGenerationBenchmark {
    private static final IDGenerator ksortableGen = new KSortableIDGenerator();

    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder()
                .include(".*" + IDLatencyGenerationBenchmark.class.getName() + ".*")
                .build())
                .run();
    }

    /**
     * Rate-limiting pacer with separate warmup and measurement rates.
     * <p>
     * During warmup, paces at a high rate to drive JIT compilation.
     * Before the measurement iteration, pauses 1 second to let the system settle,
     * then paces at the measurement rate.
     */
    public static abstract class Pacer {
        private final long warmupIntervalNs;
        private final long measurementIntervalNs;
        private static final long PAUSE_NS = 1_000_000_000L; // 1 second

        private long intervalNs;
        private long nextNs;
        private boolean warmedUp;

        protected Pacer(long warmupIntervalNs, long measurementIntervalNs) {
            this.warmupIntervalNs = warmupIntervalNs;
            this.measurementIntervalNs = measurementIntervalNs;
        }

        @Setup(Level.Iteration)
        public void reset() {
            if (!warmedUp) {
                intervalNs = warmupIntervalNs;
                warmedUp = true;
            } else {
                LockSupport.parkNanos(PAUSE_NS);
                intervalNs = measurementIntervalNs;
            }
            nextNs = System.nanoTime();
        }

        public void pace() {
            nextNs += intervalNs;
            long delta = nextNs - System.nanoTime();
            if (delta > 0) {
                LockSupport.parkNanos(delta);
            }
        }
    }

    /**
     * Pacer for 64-bit and 128-bit IDs: warmup 1,000 ops/s, measurement 1,000 ops/s.
     */
    @State(Scope.Thread)
    public static class IdPacer extends Pacer {
        public IdPacer() {
            super(1_000_000L, 1_000_000L);
        }
    }

    /**
     * Pacer for 32-bit LIDs: warmup 200 ops/s, measurement 50 ops/s.
     */
    @State(Scope.Thread)
    public static class LidPacer extends Pacer {
        public LidPacer() {
            super(5_000_000L, 20_000_000L);
        }
    }

    @Benchmark
    public LID<IDAble> ksortable_32_bit(LidPacer pacer) {
        pacer.pace();
        return ksortableGen.localIdentifier();
    }

    @Benchmark
    public ID<IDAble> ksortable_64_bit(IdPacer pacer) {
        pacer.pace();
        return ksortableGen.identifier();
    }

    @Benchmark
    public GID<IDAble> ksortable_128_bit(IdPacer pacer) {
        pacer.pace();
        return ksortableGen.globalIdentifier();
    }
}
