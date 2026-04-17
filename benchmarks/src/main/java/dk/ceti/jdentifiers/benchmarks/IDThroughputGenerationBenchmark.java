package dk.ceti.jdentifiers.benchmarks;

import dk.ceti.jdentifiers.id.GID;
import dk.ceti.jdentifiers.id.ID;
import dk.ceti.jdentifiers.id.IDAble;
import dk.ceti.jdentifiers.id.IDGenerator;
import dk.ceti.jdentifiers.id.KSortableIDGenerator;
import dk.ceti.jdentifiers.id.LID;
import dk.ceti.jdentifiers.id.RandomIDGenerator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Throughput benchmark for random ID generation.
 *
 * <p>Measures maximum generation rate without pacing. Useful for detecting
 * regressions in the ID construction and SecureRandom paths.
 *
 * <p>K-sortable generators are excluded — their throughput is bounded by
 * clock resolution and counter capacity, not by code efficiency.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1)
public class IDThroughputGenerationBenchmark {
    private static final IDGenerator randomGen = new RandomIDGenerator();
    private static final IDGenerator ksortGen = new KSortableIDGenerator();

    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder()
            .include(".*" + IDThroughputGenerationBenchmark.class.getName() + ".*")
            .build())
            .run();
    }

    @Benchmark
    public LID<IDAble> random_32_bit() {
        return randomGen.localIdentifier();
    }

    @Benchmark
    public ID<IDAble> random_64_bit() {
        return randomGen.identifier();
    }

    @Benchmark
    public GID<IDAble> random_128_bit() {
        return randomGen.globalIdentifier();
    }

    @Benchmark
    public UUID random_uuid_jdk() {
        return UUID.randomUUID();
    }

    @Benchmark
    public LID<IDAble> k_sort_32_bit() {
        return ksortGen.localIdentifier();
    }
}
