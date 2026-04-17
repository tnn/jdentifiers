package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static dk.ceti.jdentifiers.id.KSortableIDGenerator.DEFAULT_EPOCH_MS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KSortableIDDebuggerTest {

    // ---- ID (64-bit) ----

    @Test
    void debugId_no_node_bits() {
        // timestamp=60000ms, counter=5, nodeBits=0, counterBits=22
        // bits = (60000L << 22) | 5 = 0x0000003A98000005L
        ID<?> id = ID.fromLong(0x0000003A98000005L);

        String actual = KSortableIDDebugger.debugId(id, 0, DEFAULT_EPOCH_MS);

        assertEquals("""
            ID: 0000003a98000005
            ├── Timestamp: 42 bits = 60000 ms (2020-01-01T00:01:00Z)
            └── Counter:   22 bits = 5""".stripIndent(), actual);
    }

    @Test
    void debugId_with_10_node_bits() {
        // timestamp=60000ms, nodeId=42, counter=7, nodeBits=10, counterBits=12
        // bits = (60000L << 22) | (42L << 12) | 7 = 0x0000003A9802A007L
        ID<?> id = ID.fromLong(0x0000003A9802A007L);

        String actual = KSortableIDDebugger.debugId(id, 10, DEFAULT_EPOCH_MS);

        assertEquals("""
            ID: 0000003a9802a007
            ├── Timestamp: 42 bits = 60000 ms (2020-01-01T00:01:00Z)
            ├── Node:      10 bits = 42
            └── Counter:   12 bits = 7""".stripIndent(), actual);
    }

    @Test
    void debug_id_via_generator() {
        // Same bit pattern as above, but use the convenience method
        ID<?> id = ID.fromLong(0x0000003A9802A007L);

        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(stubClock())
            .nodeBits(10)
            .nodeId(42)
            .build();

        String actual = KSortableIDDebugger.debug(id, gen);

        assertEquals("""
            ID: 0000003a9802a007
            ├── Timestamp: 42 bits = 60000 ms (2020-01-01T00:01:00Z)
            ├── Node:      10 bits = 42
            └── Counter:   12 bits = 7""".stripIndent(), actual);
    }

    // ---- LID (32-bit) ----

    @Test
    void debugLid_default_epoch() {
        // hoursSinceEpoch=5, counter=3
        // bits = (5 << 12) | 3 = 0x5003
        // unixMs = 5 * 3_600_000 + DEFAULT_EPOCH_MS → 2020-01-01T05:00:00Z
        LID<?> lid = LID.fromInteger(0x00005003);

        String actual = KSortableIDDebugger.debugLid(lid, DEFAULT_EPOCH_MS);

        assertEquals("""
            LID: 00005003
            ├── Timestamp: 20 bits = 5 hours (2020-01-01T05:00:00Z)
            └── Counter:   12 bits = 3""".stripIndent(), actual);
    }

    @Test
    void debug_lid_via_generator_custom_epoch() {
        // hoursSinceEpoch=10, counter=0
        // bits = (10 << 12) = 0xA000
        Instant customEpoch = Instant.parse("2024-01-01T00:00:00Z");
        LID<?> lid = LID.fromInteger(0x0000A000);

        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(stubClock())
            .lidEpoch(customEpoch)
            .build();

        String actual = KSortableIDDebugger.debug(lid, gen);

        // 10 hours after 2024-01-01 = 2024-01-01T10:00:00Z
        assertEquals("""
            LID: 0000a000
            ├── Timestamp: 20 bits = 10 hours (2024-01-01T10:00:00Z)
            └── Counter:   12 bits = 0""".stripIndent(), actual);
    }

    // ---- GID (128-bit UUIDv7) ----

    @Test
    void debug_gid() {
        // Construct a UUIDv7 with known components:
        //   timestamp = 1000 ms (1970-01-01T00:00:01Z)
        //   version = 7
        //   rand_a (counter) = 42
        //   variant = 2
        //   rand_b = 0x123456789abcdef
        //
        // msb = (1000L << 16) | (7L << 12) | 42 = 0x0000000003E8702AL
        // lsb = 0x8000000000000000L | 0x0123456789ABCDEFL = 0x8123456789ABCDEFL
        GID<?> gid = GID.fromUuid(new UUID(0x0000000003E8702AL, 0x8123456789ABCDEFL));

        String actual = KSortableIDDebugger.debug(gid);

        assertEquals("""
            GID: 00000000-03e8-702a-8123-456789abcdef
            ├── Timestamp: 48 bits = 1000 ms (1970-01-01T00:00:01Z)
            ├── Version:   4 bits  = 7
            ├── rand_a:    12 bits = 42 (counter)
            ├── Variant:   2 bits  = 2
            └── rand_b:    62 bits = 0x123456789abcdef""".stripIndent(), actual);
    }

    @Test
    void debug_gid_large_timestamp() {
        // timestamp = 1_577_836_860_000 ms (2020-01-01T00:01:00Z)
        // counter = 0, randB = 0
        long ts = 1_577_836_860_000L;
        long msb = (ts << 16) | (7L << 12);
        long lsb = 0x8000000000000000L; // variant 10, rand_b = 0

        GID<?> gid = GID.fromUuid(new UUID(msb, lsb));

        String actual = KSortableIDDebugger.debug(gid);

        assertEquals("""
            GID: 016f5e67-d260-7000-8000-000000000000
            ├── Timestamp: 48 bits = 1577836860000 ms (2020-01-01T00:01:00Z)
            ├── Version:   4 bits  = 7
            ├── rand_a:    12 bits = 0 (counter)
            ├── Variant:   2 bits  = 2
            └── rand_b:    62 bits = 0x0""".stripIndent(), actual);
    }

    private static Clock stubClock() {
        return Clock.fixed(Instant.now(), ZoneOffset.UTC);
    }
}
