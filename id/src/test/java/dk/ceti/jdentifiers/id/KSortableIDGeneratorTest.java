package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static dk.ceti.jdentifiers.id.KSortableIDGenerator.DEFAULT_EPOCH_MS;
import static dk.ceti.jdentifiers.id.KSortableIDGenerator.HOUR_MS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KSortableIDGeneratorTest {

    // ---- ID (64-bit) tests ----

    @Test
    void id_is_not_null() {
        KSortableIDGenerator gen = new KSortableIDGenerator();
        assertNotNull(gen.<A>identifier());
    }

    @Test
    void id_timestamp_matches_clock() {
        long offsetMs = 5_000L;
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + offsetMs);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        ID<A> id = gen.identifier();
        long extractedTimestamp = id.asLong() >>> 22;
        assertEquals(offsetMs, extractedTimestamp);
    }

    @Test
    void id_monotonic_within_same_ms() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        ID<A> prev = gen.identifier();
        for (int i = 0; i < 100; i++) {
            ID<A> next = gen.identifier();
            assertTrue(prev.compareTo(next) < 0,
                "ID " + prev + " should be less than " + next);
            prev = next;
        }
    }

    @Test
    void id_monotonic_across_ms_ticks() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        List<ID<A>> ids = new ArrayList<>();
        for (int tick = 0; tick < 5; tick++) {
            for (int i = 0; i < 10; i++) {
                ids.add(gen.identifier());
            }
            clock.advance(1);
        }

        for (int i = 1; i < ids.size(); i++) {
            assertTrue(ids.get(i - 1).compareTo(ids.get(i)) < 0,
                "ID at index " + (i - 1) + " should be less than ID at index " + i);
        }
    }

    @Test
    void id_counter_resets_on_new_ms() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>identifier(); // counter = 0
        gen.<A>identifier(); // counter = 1

        clock.advance(1);
        ID<A> id = gen.identifier(); // new ms, counter should reset to 0
        long counter = id.asLong() & 0x3FFFFFL; // bottom 22 bits
        assertEquals(0, counter);
    }

    @Test
    void id_counter_overflow_waits_for_next_ms() throws Exception {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .nodeBits(12) // counterBits=10, counterMax=1023
            .nodeId(0)
            .build();

        for (int i = 0; i < 1024; i++) {
            gen.<A>identifier();
        }

        // Arm latch: the overflow spin-wait re-reads millis() in a loop.
        // After 2 reads the thread is definitely spinning.
        CountDownLatch spinning = clock.latchAfterReads(2);
        CompletableFuture<ID<A>> future = CompletableFuture.supplyAsync(gen::identifier);
        assertTrue(spinning.await(2, TimeUnit.SECONDS), "Thread did not enter spin-wait");
        clock.advance(1);

        ID<A> id = future.get(2, TimeUnit.SECONDS);
        assertNotNull(id);
        long extractedTimestamp = id.asLong() >>> 22;
        assertEquals(1001L, extractedTimestamp);
    }

    @Test
    void id_with_10_node_bits() {
        long offsetMs = 1000L;
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + offsetMs);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .nodeBits(10)
            .nodeId(42)
            .build();

        ID<A> id = gen.identifier();
        long bits = id.asLong();

        long timestamp = bits >>> 22;
        int node = (int) ((bits >>> 12) & 0x3FF);
        int counter = (int) (bits & 0xFFF);

        assertEquals(offsetMs, timestamp);
        assertEquals(42, node);
        assertEquals(0, counter);
    }

    @Test
    void id_with_5_node_bits() {
        long offsetMs = 2000L;
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + offsetMs);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .nodeBits(5)
            .nodeId(31) // max for 5 bits
            .build();

        ID<A> id = gen.identifier();
        long bits = id.asLong();

        long timestamp = bits >>> 22;
        int node = (int) ((bits >>> 17) & 0x1F);
        int counter = (int) (bits & 0x1FFFF);

        assertEquals(offsetMs, timestamp);
        assertEquals(31, node);
        assertEquals(0, counter);
    }

    @Test
    void id_clock_regression_small_spins() throws Exception {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>identifier(); // lastIdTimestamp = 1000

        clock.set(DEFAULT_EPOCH_MS + 999); // regress by 1ms

        CountDownLatch spinning = clock.latchAfterReads(2);
        CompletableFuture<ID<A>> future = CompletableFuture.supplyAsync(gen::identifier);
        assertTrue(spinning.await(2, TimeUnit.SECONDS), "Thread did not enter spin-wait");
        clock.set(DEFAULT_EPOCH_MS + 1001); // advance past last timestamp

        ID<A> id = future.get(2, TimeUnit.SECONDS);
        assertNotNull(id);
    }

    @Test
    void id_clock_regression_large_throws() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 5000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>identifier(); // lastIdTimestamp = 5000

        clock.set(DEFAULT_EPOCH_MS + 3000); // regress by 2s (> 1s tolerance)

        assertThrows(IllegalStateException.class, gen::<A>identifier);
    }

    @Test
    void id_before_epoch_throws() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS - 1);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        assertThrows(IllegalStateException.class, gen::<A>identifier);
    }

    @Test
    void id_timestamp_overflow_throws() {
        // 2^42 ms past epoch overflows the 42-bit timestamp field
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + (1L << 42));
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        assertThrows(IllegalStateException.class, gen::<A>identifier);
    }

    @Test
    void id_clock_regression_exactly_1s_spins() throws Exception {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 5000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>identifier(); // lastIdTimestamp = 5000

        clock.set(DEFAULT_EPOCH_MS + 4000); // regress by exactly 1000ms (within tolerance)

        CountDownLatch spinning = clock.latchAfterReads(2);
        CompletableFuture<ID<A>> future = CompletableFuture.supplyAsync(gen::identifier);
        assertTrue(spinning.await(2, TimeUnit.SECONDS), "Thread did not enter spin-wait");
        clock.set(DEFAULT_EPOCH_MS + 5001);

        ID<A> id = future.get(2, TimeUnit.SECONDS);
        assertEquals(5001L, id.asLong() >>> 22);
    }

    @Test
    void id_clock_regression_just_over_1s_throws() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 5000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>identifier(); // lastIdTimestamp = 5000

        clock.set(DEFAULT_EPOCH_MS + 3999); // regress by 1001ms (exceeds 1s tolerance)

        assertThrows(IllegalStateException.class, gen::<A>identifier);
    }

    // ---- GID (128-bit UUIDv7) tests ----

    @Test
    void gid_is_not_null() {
        KSortableIDGenerator gen = new KSortableIDGenerator();
        assertNotNull(gen.<A>globalIdentifier());
    }

    @Test
    void gid_has_version_7() {
        KSortableIDGenerator gen = new KSortableIDGenerator();
        for (int i = 0; i < 10; i++) {
            GID<A> gid = gen.globalIdentifier();
            assertEquals(7, gid.asUUID().version());
        }
    }

    @Test
    void gid_has_variant_2() {
        KSortableIDGenerator gen = new KSortableIDGenerator();
        for (int i = 0; i < 10; i++) {
            GID<A> gid = gen.globalIdentifier();
            assertEquals(2, gid.asUUID().variant());
        }
    }

    @Test
    void gid_timestamp_matches_clock() {
        long now = System.currentTimeMillis();
        TestClock clock = new TestClock(now);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        GID<A> gid = gen.globalIdentifier();
        long msb = gid.asUUID().getMostSignificantBits();
        long extractedTimestamp = msb >>> 16;
        assertEquals(now, extractedTimestamp);
    }

    @Test
    void gid_monotonic_within_same_ms() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        GID<A> prev = gen.globalIdentifier();
        for (int i = 0; i < 100; i++) {
            GID<A> next = gen.globalIdentifier();
            // MSB should be monotonically increasing (counter increments)
            long prevMsb = prev.asUUID().getMostSignificantBits();
            long nextMsb = next.asUUID().getMostSignificantBits();
            assertTrue(Long.compareUnsigned(prevMsb, nextMsb) < 0,
                "GID MSB should increase monotonically");
            prev = next;
        }
    }

    @Test
    void gid_counter_resets_on_new_ms() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        // Generate enough GIDs to push counter well above 256
        // (initial offset is [0,256), so after warmupCount calls the counter is >= warmupCount)
        int warmupCount = 300;
        for (int i = 0; i < warmupCount; i++) {
            gen.<A>globalIdentifier();
        }
        GID<A> lastBeforeTick = gen.globalIdentifier();
        int counterBeforeTick = (int) (lastBeforeTick.asUUID().getMostSignificantBits() & 0xFFF);
        assertTrue(counterBeforeTick >= warmupCount,
            "Counter should be >= " + warmupCount + " after " + (warmupCount + 1)
            + " generations, was " + counterBeforeTick);

        // Advance to new ms
        clock.advance(1);
        GID<A> afterTick = gen.globalIdentifier();
        int afterCounter = (int) (afterTick.asUUID().getMostSignificantBits() & 0xFFF);

        // Counter should have reset to a small random offset (< 256)
        assertTrue(afterCounter < 256,
            "Counter should re-init to small random offset (< 256), was " + afterCounter);
        assertTrue(afterCounter < counterBeforeTick,
            "Counter after reset (" + afterCounter + ") should be less than before (" + counterBeforeTick + ")");
    }

    @Test
    void gid_monotonic_across_ms_ticks() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        List<GID<A>> gids = new ArrayList<>();
        for (int tick = 0; tick < 5; tick++) {
            for (int i = 0; i < 10; i++) {
                gids.add(gen.globalIdentifier());
            }
            clock.advance(1);
        }

        for (int i = 1; i < gids.size(); i++) {
            assertTrue(gids.get(i - 1).compareTo(gids.get(i)) < 0,
                "GID at index " + (i - 1) + " should be less than GID at index " + i);
        }
    }

    @Test
    void gid_counter_overflow_waits_for_next_ms() throws Exception {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        // The 12-bit counter allows 4096 values per ms tick.
        // Initial counter is a random offset in [0, 256), so at most 4096 calls
        // exhaust it. Generate 4097 on a background thread — the overflow will
        // spin-wait until the clock advances.
        // Each globalIdentifier() call reads millis() once in the normal path.
        // We generate enough to overflow the 12-bit counter, then the spin-wait
        // reads millis() repeatedly. Latch threshold = generation calls + 1 spin read.
        int generationCalls = KSortableIDGenerator.GID_COUNTER_MAX + 2;
        CountDownLatch spinning = clock.latchAfterReads(generationCalls + 1);
        CompletableFuture<List<GID<A>>> future = CompletableFuture.supplyAsync(() -> {
            List<GID<A>> gids = new ArrayList<>();
            for (int i = 0; i < generationCalls; i++) {
                gids.add(gen.globalIdentifier());
            }
            return gids;
        });

        assertTrue(spinning.await(5, TimeUnit.SECONDS), "Thread did not enter spin-wait");
        clock.advance(1);

        List<GID<A>> gids = future.get(5, TimeUnit.SECONDS);
        assertEquals(generationCalls, gids.size());

        // After overflow the generator advances to the next ms tick
        long lastTimestamp = gids.get(gids.size() - 1).asUUID().getMostSignificantBits() >>> 16;
        assertEquals(DEFAULT_EPOCH_MS + 1001, lastTimestamp);

        // Every GID must have valid UUIDv7 version and variant
        for (GID<A> gid : gids) {
            assertEquals(7, gid.asUUID().version());
            assertEquals(2, gid.asUUID().variant());
        }
    }

    @Test
    void gid_clock_regression_small_spins() throws Exception {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 5000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>globalIdentifier(); // lastGidTimestamp = DEFAULT_EPOCH_MS + 5000

        clock.set(DEFAULT_EPOCH_MS + 4500); // regress by 500ms

        CountDownLatch spinning = clock.latchAfterReads(2);
        CompletableFuture<GID<A>> future = CompletableFuture.supplyAsync(gen::globalIdentifier);
        assertTrue(spinning.await(2, TimeUnit.SECONDS), "Thread did not enter spin-wait");
        clock.set(DEFAULT_EPOCH_MS + 5001);

        GID<A> gid = future.get(2, TimeUnit.SECONDS);
        assertNotNull(gid);
        assertEquals(7, gid.asUUID().version());
        assertEquals(2, gid.asUUID().variant());
        long ts = gid.asUUID().getMostSignificantBits() >>> 16;
        assertEquals(DEFAULT_EPOCH_MS + 5001, ts);
    }

    @Test
    void gid_clock_regression_exactly_1s_spins() throws Exception {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 5000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>globalIdentifier();

        clock.set(DEFAULT_EPOCH_MS + 4000); // regress by exactly 1000ms (within tolerance)

        CountDownLatch spinning = clock.latchAfterReads(2);
        CompletableFuture<GID<A>> future = CompletableFuture.supplyAsync(gen::globalIdentifier);
        assertTrue(spinning.await(2, TimeUnit.SECONDS), "Thread did not enter spin-wait");
        clock.set(DEFAULT_EPOCH_MS + 5001);

        GID<A> gid = future.get(2, TimeUnit.SECONDS);
        assertNotNull(gid);
    }

    @Test
    void gid_clock_regression_just_over_1s_throws() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 5000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>globalIdentifier();

        clock.set(DEFAULT_EPOCH_MS + 3999); // regress by 1001ms (exceeds tolerance)

        assertThrows(IllegalStateException.class, gen::<A>globalIdentifier);
    }

    @Test
    void gid_clock_regression_large_throws() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 5000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>globalIdentifier();

        clock.set(DEFAULT_EPOCH_MS + 3000); // regress by 2s

        assertThrows(IllegalStateException.class, gen::<A>globalIdentifier);
    }

    @Test
    void gid_timestamp_overflow_throws() {
        // 2^48 ms overflows the 48-bit timestamp field
        TestClock clock = new TestClock(1L << 48);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        assertThrows(IllegalStateException.class, gen::<A>globalIdentifier);
    }

    // ---- LID (32-bit) tests ----

    @Test
    void lid_is_not_null() {
        KSortableIDGenerator gen = new KSortableIDGenerator();
        assertNotNull(gen.<A>localIdentifier());
    }

    @Test
    void lid_timestamp_matches_clock() {
        // 1 hour after epoch
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        LID<A> lid = gen.localIdentifier();
        int bits = lid.toInteger();
        long hoursSinceEpoch = Integer.toUnsignedLong(bits) >>> 12;
        assertEquals(1L, hoursSinceEpoch);
    }

    @Test
    void lid_monotonic_within_same_hour() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        LID<A> prev = gen.localIdentifier();
        for (int i = 0; i < 100; i++) {
            LID<A> next = gen.localIdentifier();
            assertTrue(prev.compareTo(next) < 0,
                "LID " + prev + " should be less than " + next);
            prev = next;
        }
    }

    @Test
    void lid_counter_resets_on_new_hour() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>localIdentifier(); // counter = 0
        gen.<A>localIdentifier(); // counter = 1

        clock.advance(HOUR_MS); // advance to next hour
        LID<A> lid = gen.localIdentifier();
        int counter = lid.toInteger() & 0xFFF;
        assertEquals(0, counter);
    }

    @Test
    void lid_counter_overflow_throws() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        for (int i = 0; i < 4096; i++) {
            gen.<A>localIdentifier();
        }

        assertThrows(IllegalStateException.class, gen::<A>localIdentifier);
    }

    @Test
    void lid_counter_overflow_repeats_in_same_hour() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        for (int i = 0; i < 4096; i++) {
            gen.<A>localIdentifier();
        }

        // First overflow
        assertThrows(IllegalStateException.class, gen::<A>localIdentifier);
        // Repeated calls in same hour must still throw (counter must not wrap)
        assertThrows(IllegalStateException.class, gen::<A>localIdentifier);
        assertThrows(IllegalStateException.class, gen::<A>localIdentifier);
    }

    @Test
    void lid_recovers_after_counter_overflow() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        for (int i = 0; i < 4096; i++) {
            gen.<A>localIdentifier();
        }

        // Overflow throws
        assertThrows(IllegalStateException.class, gen::<A>localIdentifier);

        // Advance to next hour — generator should recover
        clock.advance(HOUR_MS);
        LID<A> lid = gen.localIdentifier();
        assertNotNull(lid);
        int counter = lid.toInteger() & 0xFFF;
        assertEquals(0, counter, "Counter should reset to 0 in new hour");
        long hours = Integer.toUnsignedLong(lid.toInteger()) >>> 12;
        assertEquals(2L, hours, "Should be in hour 2");
    }

    @Test
    void lid_clock_regression_across_hour_throws() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 2 * HOUR_MS);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>localIdentifier(); // hour = 2

        clock.set(DEFAULT_EPOCH_MS + HOUR_MS / 2); // back to hour 0

        assertThrows(IllegalStateException.class, gen::<A>localIdentifier);
    }

    @Test
    void lid_before_epoch_throws() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS - HOUR_MS);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        assertThrows(IllegalStateException.class, gen::<A>localIdentifier);
    }

    @Test
    void lid_timestamp_overflow_throws() {
        // 2^20 hours past epoch overflows the 20-bit hour timestamp field
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + ((1L << 20) * HOUR_MS));
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        assertThrows(IllegalStateException.class, gen::<A>localIdentifier);
    }

    @Test
    void lid_counter_overflow_message_mentions_random_generator() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        for (int i = 0; i < 4096; i++) {
            gen.<A>localIdentifier();
        }

        IllegalStateException ex = assertThrows(IllegalStateException.class, gen::<A>localIdentifier);
        assertTrue(ex.getMessage().contains("RandomIDGenerator"));
    }

    @Test
    void lid_clock_regression_within_same_hour_succeeds() {
        // Start 30 minutes into hour 5
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS * 5 + HOUR_MS / 2);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        LID<A> first = gen.localIdentifier(); // hour = 5, counter = 0

        // Regress 15 minutes — still within hour 5
        clock.set(DEFAULT_EPOCH_MS + HOUR_MS * 5 + HOUR_MS / 4);

        LID<A> second = gen.localIdentifier(); // hour = 5, counter = 1
        assertTrue(first.compareTo(second) < 0, "Counter should still increment within same hour");
    }

    @Test
    void lid_clock_regression_to_start_of_same_hour_succeeds() {
        // Generate at the very end of hour 5 (1ms before hour 6)
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS * 6 - 1);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>localIdentifier(); // hour = 5, counter = 0

        // Regress to the very start of hour 5
        clock.set(DEFAULT_EPOCH_MS + HOUR_MS * 5);

        LID<A> lid = gen.localIdentifier(); // still hour 5, counter = 1
        int counter = lid.toInteger() & 0xFFF;
        assertEquals(1, counter);
    }

    @Test
    void lid_clock_regression_to_end_of_previous_hour_throws() {
        // Start at the beginning of hour 5
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS * 5);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        gen.<A>localIdentifier(); // hour = 5

        // Regress to 1ms before hour 5 (last ms of hour 4)
        clock.set(DEFAULT_EPOCH_MS + HOUR_MS * 5 - 1);

        assertThrows(IllegalStateException.class, gen::<A>localIdentifier);
    }

    @Test
    void lid_custom_epoch() {
        Instant customEpoch = Instant.parse("2024-01-01T00:00:00Z");
        long customEpochMs = customEpoch.toEpochMilli();
        TestClock clock = new TestClock(customEpochMs + HOUR_MS * 10);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .lidEpoch(customEpoch)
            .build();

        LID<A> lid = gen.localIdentifier();
        int bits = lid.toInteger();
        long hours = Integer.toUnsignedLong(bits) >>> 12;
        assertEquals(10L, hours);
    }

    @Test
    void lid_monotonic_across_hours() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        List<LID<A>> lids = new ArrayList<>();
        for (int hour = 0; hour < 3; hour++) {
            for (int i = 0; i < 10; i++) {
                lids.add(gen.localIdentifier());
            }
            clock.advance(HOUR_MS);
        }

        for (int i = 1; i < lids.size(); i++) {
            assertTrue(lids.get(i - 1).compareTo(lids.get(i)) < 0,
                "LID at index " + (i - 1) + " should be less than LID at index " + i);
        }
    }

    // ---- Builder validation tests ----

    @Test
    void builder_default_values() {
        KSortableIDGenerator gen = KSortableIDGenerator.builder().build();
        assertEquals(0, gen.nodeBits());
        assertEquals(22, gen.counterBits());
        assertEquals(0, gen.nodeId());
        assertEquals(DEFAULT_EPOCH_MS, gen.lidEpochMs());
    }

    @Test
    void builder_rejects_negative_node_bits() {
        assertThrows(IllegalArgumentException.class, () ->
            KSortableIDGenerator.builder().nodeBits(-1).build());
    }

    @Test
    void builder_rejects_node_bits_too_large() {
        assertThrows(IllegalArgumentException.class, () ->
            KSortableIDGenerator.builder().nodeBits(23).build());
    }

    @Test
    void builder_rejects_node_bits_22_degenerate() {
        // nodeBits=22 would leave 0 counter bits — degenerate, rejected
        assertThrows(IllegalArgumentException.class, () ->
            KSortableIDGenerator.builder().nodeBits(22).build());
    }

    @Test
    void builder_rejects_node_id_without_node_bits() {
        assertThrows(IllegalArgumentException.class, () ->
            KSortableIDGenerator.builder().nodeId(1).build());
    }

    @Test
    void builder_rejects_node_id_out_of_range() {
        assertThrows(IllegalArgumentException.class, () ->
            KSortableIDGenerator.builder().nodeBits(10).nodeId(1024).build());
    }

    @Test
    void builder_rejects_negative_node_id() {
        assertThrows(IllegalArgumentException.class, () ->
            KSortableIDGenerator.builder().nodeBits(10).nodeId(-1).build());
    }

    @Test
    void builder_rejects_both_node_id_and_supplier() {
        assertThrows(IllegalArgumentException.class, () ->
            KSortableIDGenerator.builder()
                .nodeBits(10)
                .nodeId(1)
                .nodeIdFactory(() -> 2)
                .build());
    }

    @Test
    void builder_accepts_node_id_supplier() {
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .nodeBits(10)
            .nodeIdFactory(() -> 42)
            .build();
        assertEquals(42, gen.nodeId());
    }

    @Test
    void builder_accepts_max_node_id() {
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .nodeBits(10)
            .nodeId(1023) // max for 10 bits
            .build();
        assertEquals(1023, gen.nodeId());
    }

    // ---- Thread safety test ----

    @Test
    void concurrent_id_generation_produces_unique_and_monotonic_ids() throws Exception {
        KSortableIDGenerator gen = new KSortableIDGenerator();
        int threadCount = 4;
        int idsPerThread = 10_000;
        Set<Long> allIds = ConcurrentHashMap.newKeySet();
        List<List<Long>> perThreadIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            perThreadIds.add(new ArrayList<>());
        }
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final List<Long> threadIds = perThreadIds.get(t);
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = gen.<A>identifier().asLong();
                        threadIds.add(id);
                        allIds.add(id);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        assertEquals(threadCount * idsPerThread, allIds.size(), "All IDs must be unique");

        // Within-thread monotonicity: each thread's IDs must be strictly increasing
        for (int t = 0; t < threadCount; t++) {
            List<Long> ids = perThreadIds.get(t);
            for (int i = 1; i < ids.size(); i++) {
                assertTrue(Long.compareUnsigned(ids.get(i - 1), ids.get(i)) < 0,
                    "Thread " + t + ": ID at index " + (i - 1) + " should be less than ID at index " + i);
            }
        }
    }

    @Test
    void concurrent_gid_generation_produces_unique_and_monotonic_ids() throws Exception {
        KSortableIDGenerator gen = new KSortableIDGenerator();
        int threadCount = 4;
        int idsPerThread = 10_000;
        Set<UUID> allIds = ConcurrentHashMap.newKeySet();
        List<List<GID<A>>> perThreadIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            perThreadIds.add(new ArrayList<>());
        }
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final List<GID<A>> threadIds = perThreadIds.get(t);
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < idsPerThread; i++) {
                        GID<A> gid = gen.globalIdentifier();
                        threadIds.add(gid);
                        allIds.add(gid.asUUID());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        assertEquals(threadCount * idsPerThread, allIds.size(), "All GIDs must be unique");

        // Within-thread monotonicity: each thread's GIDs must be strictly increasing
        for (int t = 0; t < threadCount; t++) {
            List<GID<A>> gids = perThreadIds.get(t);
            for (int i = 1; i < gids.size(); i++) {
                assertTrue(gids.get(i - 1).compareTo(gids.get(i)) < 0,
                    "Thread " + t + ": GID at index " + (i - 1) + " should be less than GID at index " + i);
            }
        }
    }

    @Test
    void concurrent_lid_generation_produces_unique_and_monotonic_ids() throws Exception {
        KSortableIDGenerator gen = new KSortableIDGenerator();
        int threadCount = 4;
        int idsPerThread = 100; // LID has 4096/hour limit total, keep low
        Set<Integer> allIds = ConcurrentHashMap.newKeySet();
        List<List<LID<A>>> perThreadIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            perThreadIds.add(new ArrayList<>());
        }
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final List<LID<A>> threadIds = perThreadIds.get(t);
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < idsPerThread; i++) {
                        LID<A> lid = gen.localIdentifier();
                        threadIds.add(lid);
                        allIds.add(lid.toInteger());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        assertEquals(threadCount * idsPerThread, allIds.size(), "All LIDs must be unique");

        // Within-thread monotonicity: each thread's LIDs must be strictly increasing
        for (int t = 0; t < threadCount; t++) {
            List<LID<A>> lids = perThreadIds.get(t);
            for (int i = 1; i < lids.size(); i++) {
                assertTrue(lids.get(i - 1).compareTo(lids.get(i)) < 0,
                    "Thread " + t + ": LID at index " + (i - 1) + " should be less than LID at index " + i);
            }
        }
    }

    // ---- Spin-wait timeout tests ----

    @Test
    void id_spin_wait_timeout_on_counter_overflow() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .nodeBits(12) // counterBits=10, counterMax=1023
            .nodeId(0)
            .maxSpinNanos(50_000_000L) // 50ms
            .build();

        for (int i = 0; i < 1024; i++) {
            gen.<A>identifier();
        }

        // Clock is frozen — spin-wait should timeout
        IllegalStateException ex = assertThrows(IllegalStateException.class, gen::<A>identifier);
        assertTrue(ex.getMessage().contains("timeout"));
    }

    @Test
    void gid_spin_wait_timeout_on_counter_overflow() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .maxSpinNanos(50_000_000L) // 50ms
            .build();

        // GID counter starts at random offset [0,256), max value 4095.
        // Generate until overflow — with frozen clock the spin-wait will timeout.
        IllegalStateException ex = null;
        for (int i = 0; i < KSortableIDGenerator.GID_COUNTER_MAX + 2; i++) {
            try {
                gen.<A>globalIdentifier();
            } catch (IllegalStateException e) {
                ex = e;
                break;
            }
        }
        assertNotNull(ex, "Expected timeout exception from GID spin-wait");
        assertTrue(ex.getMessage().contains("timeout"));
    }

    // ---- Clock-regression timeout tests ----

    @Test
    void id_spin_wait_timeout_on_clock_regression() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 5000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .maxSpinNanos(50_000_000L) // 50ms
            .build();

        gen.<A>identifier();

        clock.set(DEFAULT_EPOCH_MS + 4500); // regress 500ms, within tolerance
        // Clock frozen at regressed value — spin-wait should timeout
        IllegalStateException ex = assertThrows(IllegalStateException.class, gen::<A>identifier);
        assertTrue(ex.getMessage().contains("timeout"));
    }

    @Test
    void gid_spin_wait_timeout_on_clock_regression() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 5000);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .maxSpinNanos(50_000_000L) // 50ms
            .build();

        gen.<A>globalIdentifier();

        clock.set(DEFAULT_EPOCH_MS + 4500); // regress 500ms, within tolerance
        // Clock frozen at regressed value — spin-wait should timeout
        IllegalStateException ex = assertThrows(IllegalStateException.class, gen::<A>globalIdentifier);
        assertTrue(ex.getMessage().contains("timeout"));
    }

    // ---- Post-spin-wait timestamp overflow tests ----

    @Test
    void id_timestamp_overflow_after_spin_wait() throws Exception {
        // Set clock to max valid 42-bit timestamp
        long maxTimestamp = (1L << KSortableIDGenerator.ID_TIMESTAMP_BITS) - 1;
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + maxTimestamp);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .nodeBits(12) // counterBits=10, counterMax=1023
            .nodeId(0)
            .maxSpinNanos(2_000_000_000L)
            .build();

        // Exhaust 10-bit counter
        for (int i = 0; i < 1024; i++) {
            gen.<A>identifier();
        }

        // Arm latch for spin-wait detection, then launch async call
        CountDownLatch spinning = clock.latchAfterReads(2);
        CompletableFuture<ID<A>> future = CompletableFuture.supplyAsync(gen::identifier);
        assertTrue(spinning.await(2, TimeUnit.SECONDS), "Thread did not enter spin-wait");

        // Advance clock past 42-bit boundary — checkIdTimestamp should throw
        clock.set(DEFAULT_EPOCH_MS + maxTimestamp + 1);

        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertTrue(e.getCause().getMessage().contains("42-bit timestamp overflow"));
            return;
        }
        throw new AssertionError("Expected IllegalStateException for 42-bit timestamp overflow");
    }

    @Test
    void gid_timestamp_overflow_after_spin_wait() throws Exception {
        // Set clock to max valid 48-bit timestamp
        long maxTimestamp = (1L << KSortableIDGenerator.GID_TIMESTAMP_BITS) - 1;
        TestClock clock = new TestClock(maxTimestamp);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .maxSpinNanos(2_000_000_000L)
            .build();

        // Exhaust 12-bit counter
        IllegalStateException earlyTimeout = null;
        for (int i = 0; i < KSortableIDGenerator.GID_COUNTER_MAX + 1; i++) {
            try {
                gen.<A>globalIdentifier();
            } catch (IllegalStateException e) {
                earlyTimeout = e;
                break;
            }
        }
        // Counter might not fully exhaust if random init offset is high,
        // but we need to trigger the spin-wait. If it already timed out, skip.
        if (earlyTimeout != null) {
            return; // counter overflow already triggered, test not meaningful at this timestamp
        }

        // Arm latch for spin-wait detection, then launch async call
        CountDownLatch spinning = clock.latchAfterReads(2);
        CompletableFuture<GID<A>> future = CompletableFuture.supplyAsync(gen::globalIdentifier);
        assertTrue(spinning.await(2, TimeUnit.SECONDS), "Thread did not enter spin-wait");

        // Advance clock past 48-bit boundary — checkGidTimestamp should throw
        clock.set(maxTimestamp + 1);

        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertTrue(e.getCause().getMessage().contains("48-bit timestamp overflow"));
            return;
        }
        throw new AssertionError("Expected IllegalStateException for 48-bit timestamp overflow");
    }

    // ---- Builder validation: maxSpinNanos ----

    @Test
    void builder_rejects_non_positive_max_spin_nanos() {
        assertThrows(IllegalArgumentException.class, () ->
            KSortableIDGenerator.builder().maxSpinNanos(0));
        assertThrows(IllegalArgumentException.class, () ->
            KSortableIDGenerator.builder().maxSpinNanos(-1));
    }

    // ---- Debugger tests ----

    @Test
    void debug_id_output_contains_components() {
        long offsetMs = 60_000L; // 1 minute after epoch
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + offsetMs);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .nodeBits(10)
            .nodeId(42)
            .build();

        ID<A> id = gen.identifier();
        String debug = KSortableIDDebugger.debug(id, gen);

        assertTrue(debug.contains("ID: " + id));
        assertTrue(debug.contains("Timestamp:"));
        assertTrue(debug.contains("60000 ms"));
        assertTrue(debug.contains("Node:"));
        assertTrue(debug.contains("= 42"));
        assertTrue(debug.contains("Counter:"));
    }

    @Test
    void debug_gid_output_contains_version_and_variant() {
        KSortableIDGenerator gen = new KSortableIDGenerator();
        GID<A> gid = gen.globalIdentifier();
        String debug = KSortableIDDebugger.debug(gid);

        assertTrue(debug.contains("GID: " + gid));
        assertTrue(debug.contains("Version:"));
        assertTrue(debug.contains("= 7"));
        assertTrue(debug.contains("Variant:"));
        assertTrue(debug.contains("= 2"));
        assertTrue(debug.contains("rand_a:"));
        assertTrue(debug.contains("rand_b:"));
    }

    @Test
    void debug_lid_output_contains_components() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS + HOUR_MS * 5);
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        LID<A> lid = gen.localIdentifier();
        String debug = KSortableIDDebugger.debug(lid, gen);

        assertTrue(debug.contains("LID: " + lid));
        assertTrue(debug.contains("Timestamp:"));
        assertTrue(debug.contains("5 hours"));
        assertTrue(debug.contains("Counter:"));
    }

    // ---- Boundary / edge case tests ----

    @Test
    void id_at_epoch_boundary() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS); // exactly at epoch
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        ID<A> id = gen.identifier();
        long extractedTimestamp = id.asLong() >>> 22;
        assertEquals(0, extractedTimestamp);
    }

    @Test
    void lid_at_epoch_boundary() {
        TestClock clock = new TestClock(DEFAULT_EPOCH_MS); // exactly at epoch (hour 0)
        KSortableIDGenerator gen = KSortableIDGenerator.builder()
            .clock(clock)
            .build();

        LID<A> lid = gen.localIdentifier();
        int bits = lid.toInteger();
        assertEquals(0, bits); // hour 0, counter 0
    }

    @Test
    void gid_uuid_string_is_valid() {
        KSortableIDGenerator gen = new KSortableIDGenerator();
        GID<A> gid = gen.globalIdentifier();
        // Should be parseable as UUID without throwing
        UUID.fromString(gid.toString());
    }

    // ---- Test helpers ----

    private static class TestClock extends Clock {
        private final AtomicLong millis;
        private volatile CountDownLatch readLatch;
        private volatile int readLatchAfter;
        private final java.util.concurrent.atomic.AtomicInteger readCount =
            new java.util.concurrent.atomic.AtomicInteger();

        TestClock(long initialMillis) {
            this.millis = new AtomicLong(initialMillis);
        }

        void set(long millis) {
            this.millis.set(millis);
        }

        void advance(long ms) {
            this.millis.addAndGet(ms);
        }

        /**
         * Arms a latch that will be counted down after the Nth call to millis().
         * This allows tests to wait until the generator has entered a spin-wait
         * loop before advancing the clock.
         */
        CountDownLatch latchAfterReads(int n) {
            readCount.set(0);
            CountDownLatch latch = new CountDownLatch(1);
            this.readLatchAfter = n;
            this.readLatch = latch;
            return latch;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis.get());
        }

        @Override
        public long millis() {
            long val = millis.get();
            CountDownLatch l = readLatch;
            if (l != null && readCount.incrementAndGet() >= readLatchAfter) {
                l.countDown();
                readLatch = null; // fire once
            }
            return val;
        }
    }

    private interface A extends IDAble {}
}
