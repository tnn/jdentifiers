package dk.ceti.jdentifiers.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import dk.ceti.jdentifiers.id.GID;
import dk.ceti.jdentifiers.id.ID;
import dk.ceti.jdentifiers.id.IDAble;
import dk.ceti.jdentifiers.id.IDGenerator;
import dk.ceti.jdentifiers.id.LID;
import dk.ceti.jdentifiers.id.RandomIDGenerator;

import java.util.UUID;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1)
public class IDGenerationBenchmark {
    private static final IDGenerator randomGen = new RandomIDGenerator();

    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder()
            .include(".*" + IDGenerationBenchmark.class.getName() + ".*")
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
}
