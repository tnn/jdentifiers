package org.pkgd.jdentifiers.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.pkgd.jdentifiers.id.GID;
import org.pkgd.jdentifiers.id.ID;
import org.pkgd.jdentifiers.id.IDAble;
import org.pkgd.jdentifiers.id.IDGenerator;
import org.pkgd.jdentifiers.id.KSortableIDGenerator;
import org.pkgd.jdentifiers.id.LID;
import org.pkgd.jdentifiers.id.RandomIDGenerator;

import java.time.Clock;
import java.util.UUID;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1)
public class IDGenerationBenchmark {
    private static final IDGenerator randomGen = new RandomIDGenerator();
    private static final IDGenerator ksortedGen = new KSortableIDGenerator(Clock.systemUTC());

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

    @Benchmark
    public ID<IDAble> k_sorted_64_bit() {
        return ksortedGen.identifier();
    }
}
