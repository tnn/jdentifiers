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
import dk.ceti.jdentifiers.id.LID;

import java.util.UUID;

/**
 * Benchmark string conversion for the various ID implementations.
 * <p>
 * Each {@code @Benchmark} method returns its result so JMH implicitly consumes it,
 * preventing dead-code elimination without an explicit {@code Blackhole}.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1)
public class IDStringBenchmark implements IDAble {
    private static final LID<IDStringBenchmark> ID_32 = LID.fromString("6a677fc2");
    private static final ID<IDStringBenchmark> ID_64 = ID.fromString("6a677fc2ee05e1f6");

    private static final GID<IDStringBenchmark> ID_128 = GID.fromString("420bb7c1-4bb6-4936-9ab1-b6b81f9c0f61");
    private static final UUID TEST_UUID = UUID.fromString("eee0d1bc-867e-4f08-99cc-35334bb3fee9");

    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder()
                .include(".*" + IDStringBenchmark.class.getName() + ".*")
                .build())
                .run();
    }

    @Benchmark
    public LID<IDAble> lid_32_bit_from_integer() {
        return LID.fromInteger(2_047_483_647);
    }

    @Benchmark
    public LID<IDAble> lid_32_bit_from_base16_string() {
        return LID.fromString("6a677fc3");
    }

    @Benchmark
    public LID<IDAble> lid_32_bit_from_base16_upper_case() {
        return LID.fromString("6A677FC4");
    }

    @Benchmark
    public String lid_32_bit_to_base16_string() {
        return ID_32.toString();
    }

    @Benchmark
    public ID<IDAble> id_64_bit_from_long() {
        return ID.fromLong(9_223_372_036_854_775_807L);
    }

    @Benchmark
    public ID<IDAble> id_64_bit_from_base16_string() {
        return ID.fromString("6a677fc2ee05e1f7");
    }

    @Benchmark
    public ID<IDAble> id_64_bit_from_base16_upper_case() {
        return ID.fromString("6A677FC2EE05E1F8");
    }

    @Benchmark
    public String id_64_bit_to_base16_string() {
        return ID_64.toString();
    }

    @Benchmark
    public String id_64_bit_to_base64_string() {
        return ID_64.toBase64String();
    }

    @Benchmark
    public GID<IDAble> gid_128_bit_from_uuid() {
        return GID.fromUuid(TEST_UUID);
    }

    @Benchmark
    public GID<IDAble> gid_128_bit_from_base16_string() {
        return GID.fromString("de83dd89-d106-406c-8eff-53864a4b2d13");
    }

    @Benchmark
    public String gid_128_bit_to_base16_string() {
        return ID_128.toString();
    }

    @Benchmark
    public UUID jdk_uuid_from_upper_case_string() {
        return UUID.fromString("9FA73CAA-2F6A-4EF7-B868-154CE3BE68EF");
    }

    @Benchmark
    public UUID jdk_uuid_from_string() {
        return UUID.fromString("6f696d11-c46b-4800-b26d-6cdc452ecee6");
    }

    @Benchmark
    public String jdk_uuid_to_string() {
        return TEST_UUID.toString();
    }
}
