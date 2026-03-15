# Round 4: Staff-Engineer Hardening Pass

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate all remaining correctness risks, test defects, and mechanical-sympathy concerns surfaced by an adversarial audit of the post-Round-3 codebase.

**Architecture:** Fixes are organized into four categories — (A) test defects that mask real gaps, (B) concurrency/mechanical concerns, (C) defensive hardening of production code, (D) observability for operators. Each task is self-contained and independently shippable.

**Tech Stack:** Java 17+ (tested on JDK 21), JUnit 5, JMH 1.37, Maven.

---

## Audit Findings (Ranked)

Below is the complete set of findings, with severity, before the task breakdown.
Items marked **[PLAN]** have a task. Items marked **[ACCEPT]** are deliberate design trade-offs documented here for the record.

### P0 — Test defect masking real coverage gap

| # | Finding | Detail |
|---|---------|--------|
| F1 | `gid_timestamp_overflow_after_spin_wait` is effectively dead code | The GID 12-bit counter starts at a random offset `r` in `[0, 256)`. The for-loop generates `GID_COUNTER_MAX + 1` (4096) iterations. When `r > 0` (probability 255/256 = **99.6%**), the counter overflows *during* the loop, the clock is frozen, and the 2-second `maxSpinNanos` burns before an `IllegalStateException` exits via the catch. The test returns early without ever reaching the async spin-wait + overflow path. Only when `r = 0` (~0.4%) does the test exercise its intended code. **Measured: 2.061s wall-clock time confirms early-timeout path.** |

### P1 — Should fix before wide adoption

| # | Finding | Detail |
|---|---------|--------|
| F2 | Spin-wait holds `synchronized` lock for up to `maxSpinNanos` (default 2s) | While one thread spin-waits on counter overflow or clock regression, **all** other threads calling the same method block on the monitor. For `identifier()`: a single counter overflow event at 4M+ IDs/sec stalls the entire pool for up to 1ms (next tick). For clock regression: up to 1 second. This is bounded but severe under high contention. |
| F3 | Pure `Thread.onSpinWait()` spin burns a full CPU core | Counter overflow resolves at the next ms tick (≤1ms). Clock regression resolves when NTP corrects (could be hundreds of ms). The pure spin is appropriate for the <1ms counter-overflow case but wasteful for the regression case. A hybrid spin-then-park would be more scheduler-friendly. |
| F4 | No test for generator state recovery after exception | After a timeout throw, the counter is frozen at `counterMax + 1`. The next call must recover (either same-tick → re-enter spin-wait, or new-tick → reset counter to 0). This state machine is correct by code inspection but has zero test coverage. If a future refactor breaks recovery, nothing catches it. |
| F5 | No upper bound on `maxSpinNanos` | `Long.MAX_VALUE` (292 years) is accepted. Combined with F2, this means a misconfigured generator can hold a lock forever. |

### P2 — Good to fix

| # | Finding | Detail |
|---|---------|--------|
| F6 | False sharing between mutable state groups | The three lock objects (`idLock`, `gidLock`, `lidLock`) enable concurrent access to different ID types. But the guarded mutable fields (`lastIdTimestamp`/`idCounter`, `lastGidTimestamp`/`gidCounter`, `lastLidHour`/`lidCounter`) likely share a cache line (total: 36 bytes of mutable state, object header + final fields put them within 128 bytes of each other). Writing under `idLock` invalidates the line for a thread holding `gidLock`. The separate-lock optimization is partially defeated. |
| F7 | `random.nextInt(GID_COUNTER_INIT_BOUND)` called inside `gidLock` | On every new-ms transition (~1000x/sec), the critical section includes a SecureRandom call. DRBG/SHA1PRNG have internal synchronization. This adds PRNG latency to a lock-held path. |
| F8 | `RandomIDGenerator.numberGenerator` is a static shared `SecureRandom` | All `RandomIDGenerator` instances (and all threads) share a single PRNG. SHA1PRNG/DRBG use internal synchronization → contention bottleneck under multi-threaded random ID generation. |
| F9 | Magic numbers in GID MSB bit-assembly | Line 249: `(now << 16) | (0x7L << 12) | (gidCounter & 0xFFF)` — the `16`, `0x7L`, `12`, `0xFFF` are raw UUIDv7 layout constants. Compare with the fully-named constants used everywhere else. |
| F10 | GID LSB variant/random masks are raw hex | Line 204: `0x3FFFFFFFFFFFFFFFL` and `0x8000000000000000L` — these are RFC 9562 variant bits, not self-documenting. |
| F11 | No `toString()` on `KSortableIDGenerator` | In production, logging a generator instance for diagnostics shows `KSortableIDGenerator@hash`. No configuration visible — makes debugging blind. |
| F12 | `KSortableIDDebugger.debug(GID)` hardcodes `48` at line 103 | Same inconsistency class as the Round 3 fix to `checkGidTimestamp`, but in the debugger output string. |

### P3 — Minor / cosmetic

| # | Finding | Detail |
|---|---------|--------|
| F13 | `lastIdTimestamp` stores epoch-relative ms; `lastGidTimestamp` stores absolute Unix ms | Correct but the field names don't convey the difference. A maintainer reading both methods could assume they use the same reference frame. |
| F14 | `GID_COUNTER_MAX = 0xFFF` uses hex literal; `LID_COUNTER_MAX = (1 << LID_COUNTER_BITS) - 1` derives from bit-width | Cosmetic inconsistency. Both are 4095. |
| F15 | No test for concurrent mixed-type generation | Tests cover concurrent `identifier()` and concurrent `globalIdentifier()` separately, but not simultaneous calls to both methods from different threads. The separate-lock design exists precisely for this scenario. |
| F16 | `GID_COUNTER_INIT_BOUND = 256` has no comment explaining why 256 | RFC 9562 Section 6.2 recommends "a small random value" but doesn't prescribe a range. The choice of 256 (8 bits) is a balance between collision avoidance across generators and counter space utilization. |

### [ACCEPT] — Deliberate design trade-offs (no action)

| # | Finding | Rationale |
|---|---------|-----------|
| A1 | Per-instance `SecureRandom` on `KSortableIDGenerator` vs shared | Per-instance is intentional: provides test isolation, avoids cross-instance contention. Entropy cost at construction is one-time and amortized. |
| A2 | Wasted `random.nextLong()` under GID contention | Pre-computed outside lock to reduce critical section. Wasted when thread loses contention race. Cost: one PRNG call (~200ns). Negligible vs. the alternative of PRNG-inside-lock on the hot path. |
| A3 | `synchronized` blocks (vs `ReentrantLock`) | `synchronized` is simpler, JIT-optimized (biased locking on some JVMs, thin locks on others). `ReentrantLock` with `Condition.await` would allow parking during spin-wait, but the spin-wait already holds the lock — `await()` would release it, requiring complex re-validation. The simplicity wins. |
| A4 | Allocation per ID (wrapper objects) | Inherent to phantom-type wrappers. `ID` = 16 bytes (header + long), `GID` = 16 bytes (header + UUID ref) + `UUID` = 32 bytes. Eden-collected. Project Valhalla value types will eventually eliminate this. Not actionable on JDK 17-21. |
| A5 | `UUID.randomUUID()` in `RandomIDGenerator.globalIdentifier()` vs own PRNG | Deliberate — canonical UUIDv4, correct variant/version bits, JDK's ThreadLocalRandom-backed implementation is lock-free on JDK 17+. |

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `id/src/main/java/.../KSortableIDGenerator.java` | Modify | F3 (hybrid spin), F5 (maxSpinNanos cap), F9/F10 (constants), F11 (toString), F13 (field rename) |
| `id/src/main/java/.../KSortableIDDebugger.java` | Modify | F12 (hardcoded 48) |
| `id/src/test/java/.../KSortableIDGeneratorTest.java` | Modify | F1 (GID overflow test), F4 (recovery tests), F15 (mixed-type concurrency) |

Files **not** changed (and why):
- `RandomIDGenerator.java` — F8 (shared PRNG) is a valid concern but changing to per-instance allocation changes the class's threading contract and memory footprint. Deferred to a separate plan with benchmark comparison.
- `SecureRandoms.java` — no findings.
- `ID.java`, `LID.java`, `GID.java`, `HexCodec.java` — no findings.

---

## Chunk 1: Test Defects and Coverage Gaps (F1, F4, F15)

These must come first — they establish the test infrastructure that validates production fixes.

### Task 1: Fix `gid_timestamp_overflow_after_spin_wait` (F1)

**Problem:** The test is non-deterministic and exercises its intended path only ~0.4% of the time, while burning 2 seconds of CPU on the other 99.6%.

**Root cause:** The GID counter starts at a random offset `r`. The for-loop can't know the exact call count needed to exhaust without triggering the spin-wait. When the spin-wait triggers mid-loop, the clock is frozen, so it burns `maxSpinNanos` before throwing.

**Fix strategy:** Instead of guessing the loop count, observe the counter value in each generated GID. Stop when the counter reaches `GID_COUNTER_MAX`. The next call will trigger the overflow spin-wait deterministically.

**Files:**
- Modify: `id/src/test/java/dk/ceti/jdentifiers/id/KSortableIDGeneratorTest.java` (the `gid_timestamp_overflow_after_spin_wait` method, approximately lines 1071-1113)

- [ ] **Step 1: Rewrite the test to pump-until-max**

Replace the entire `gid_timestamp_overflow_after_spin_wait` method body with:

```java
@Test
void gid_timestamp_overflow_after_spin_wait() throws Exception {
    // Set clock to max valid 48-bit timestamp
    long maxTimestamp = (1L << KSortableIDGenerator.GID_TIMESTAMP_BITS) - 1;
    TestClock clock = new TestClock(maxTimestamp);
    KSortableIDGenerator gen = KSortableIDGenerator.builder()
        .clock(clock)
        .maxSpinNanos(2_000_000_000L)
        .build();

    // Pump counter to GID_COUNTER_MAX by observing the output.
    // Each call increments the counter. Stop when we see 4095 in the GID.
    int counter;
    do {
        GID<A> gid = gen.globalIdentifier();
        counter = (int) (gid.asUUID().getMostSignificantBits() & 0xFFF);
    } while (counter < KSortableIDGenerator.GID_COUNTER_MAX);

    // Counter is now at GID_COUNTER_MAX. The next call will increment to
    // GID_COUNTER_MAX + 1 and enter the spin-wait.
    CountDownLatch spinning = clock.latchAfterReads(2);
    CompletableFuture<GID<A>> future = CompletableFuture.supplyAsync(gen::globalIdentifier);
    assertTrue(spinning.await(2, TimeUnit.SECONDS), "Thread did not enter spin-wait");

    // Advance clock past 48-bit boundary — checkGidTimestamp should throw
    clock.set(maxTimestamp + 1);

    java.util.concurrent.ExecutionException ee = assertThrows(
        java.util.concurrent.ExecutionException.class,
        () -> future.get(5, TimeUnit.SECONDS));
    assertInstanceOf(IllegalStateException.class, ee.getCause());
    assertTrue(ee.getCause().getMessage().contains("48-bit timestamp overflow"));
}
```

- [ ] **Step 2: Run test to verify it passes quickly**

Run: `mvn -pl id test -Dtest=KSortableIDGeneratorTest#gid_timestamp_overflow_after_spin_wait`
Expected: PASS in <1s (no more 2-second timeout)

- [ ] **Step 3: Also fix `id_timestamp_overflow_after_spin_wait` to use `assertThrows` pattern**

Replace the manual try/catch/throw in the ID variant with the same clean `assertThrows(ExecutionException.class, ...)` pattern. Also fix the typo `AssertionError` → this is actually correct Java (`java.lang.AssertionError`) but the pattern is still inferior to `assertThrows`. Replace:

```java
// BEFORE: manual try/catch
try {
    future.get(5, TimeUnit.SECONDS);
} catch (java.util.concurrent.ExecutionException e) {
    assertTrue(e.getCause() instanceof IllegalStateException);
    assertTrue(e.getCause().getMessage().contains("42-bit timestamp overflow"));
    return;
}
throw new AssertionError("Expected IllegalStateException for 42-bit timestamp overflow");

// AFTER: clean assertThrows
java.util.concurrent.ExecutionException ee = assertThrows(
    java.util.concurrent.ExecutionException.class,
    () -> future.get(5, TimeUnit.SECONDS));
assertInstanceOf(IllegalStateException.class, ee.getCause());
assertTrue(ee.getCause().getMessage().contains("42-bit timestamp overflow"));
```

- [ ] **Step 4: Add `assertInstanceOf` import if missing**

Add to the static imports at the top of the test file:
```java
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
```

- [ ] **Step 5: Run both tests to verify**

Run: `mvn -pl id test -Dtest="KSortableIDGeneratorTest#gid_timestamp_overflow_after_spin_wait+id_timestamp_overflow_after_spin_wait"`
Expected: Both PASS, each in <1s

- [ ] **Step 6: Commit**

```bash
git add id/src/test/java/dk/ceti/jdentifiers/id/KSortableIDGeneratorTest.java
git commit -m "fix(test): make GID overflow-after-spin-wait test deterministic

The test exercised its intended code path only ~0.4% of the time
(when random counter init happened to be 0). The other 99.6%, it
burned 2 seconds on a timeout and silently returned early.

Fix: pump the counter by observing GID output until it reaches
GID_COUNTER_MAX, then trigger the overflow deterministically.
Also clean up both overflow tests to use assertThrows pattern."
```

---

### Task 2: Test generator recovery after exception (F4)

**Problem:** The counter-guard fix from Round 3 ensures `idCounter`/`gidCounter` freeze at `counterMax + 1` after a timeout. Subsequent calls in the same tick should re-enter the spin-wait (not produce garbage IDs). Calls after the clock advances should reset normally. This state machine is untested.

**Files:**
- Modify: `id/src/test/java/dk/ceti/jdentifiers/id/KSortableIDGeneratorTest.java`

- [ ] **Step 1: Write test for ID recovery after timeout**

Add after the spin-wait timeout tests section:

```java
@Test
void id_recovers_after_spin_wait_timeout() {
    TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
    KSortableIDGenerator gen = KSortableIDGenerator.builder()
        .clock(clock)
        .nodeBits(12) // counterBits=10, counterMax=1023
        .nodeId(0)
        .maxSpinNanos(50_000_000L) // 50ms
        .build();

    // Exhaust counter
    for (int i = 0; i < 1024; i++) {
        gen.<A>identifier();
    }

    // First timeout — counter frozen at 1024
    assertThrows(IllegalStateException.class, gen::<A>identifier);

    // Second call in same tick — should re-enter spin-wait and timeout again,
    // NOT produce a garbage ID with counter > counterMax
    assertThrows(IllegalStateException.class, gen::<A>identifier);

    // Advance clock — generator should recover
    clock.advance(1);
    ID<A> recovered = gen.identifier();
    assertNotNull(recovered);
    long extractedTimestamp = recovered.asLong() >>> 22;
    assertEquals(1001L, extractedTimestamp);
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `mvn -pl id test -Dtest=KSortableIDGeneratorTest#id_recovers_after_spin_wait_timeout`
Expected: PASS

- [ ] **Step 3: Write test for GID recovery after timeout**

```java
@Test
void gid_recovers_after_spin_wait_timeout() {
    TestClock clock = new TestClock(DEFAULT_EPOCH_MS + 1000);
    KSortableIDGenerator gen = KSortableIDGenerator.builder()
        .clock(clock)
        .maxSpinNanos(50_000_000L) // 50ms
        .build();

    // Exhaust counter by observing output
    int counter;
    do {
        GID<A> gid = gen.globalIdentifier();
        counter = (int) (gid.asUUID().getMostSignificantBits() & 0xFFF);
    } while (counter < KSortableIDGenerator.GID_COUNTER_MAX);

    // First timeout — counter frozen at GID_COUNTER_MAX + 1
    assertThrows(IllegalStateException.class, gen::<A>globalIdentifier);

    // Second call in same tick — should timeout again, not produce garbage
    assertThrows(IllegalStateException.class, gen::<A>globalIdentifier);

    // Advance clock — generator should recover
    clock.advance(1);
    GID<A> recovered = gen.globalIdentifier();
    assertNotNull(recovered);
    assertEquals(7, recovered.asUUID().version());
    assertEquals(2, recovered.asUUID().variant());
    long ts = recovered.asUUID().getMostSignificantBits() >>> 16;
    assertEquals(DEFAULT_EPOCH_MS + 1001, ts);
}
```

- [ ] **Step 4: Run test to verify**

Run: `mvn -pl id test -Dtest=KSortableIDGeneratorTest#gid_recovers_after_spin_wait_timeout`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add id/src/test/java/dk/ceti/jdentifiers/id/KSortableIDGeneratorTest.java
git commit -m "test: verify generator recovers after spin-wait timeout

Covers the state machine: timeout leaves counter frozen at counterMax+1,
subsequent same-tick calls re-enter spin-wait (not garbage), and clock
advance resets the counter normally."
```

---

### Task 3: Mixed-type concurrent generation test (F15)

**Problem:** The separate-lock design exists to prevent cross-type head-of-line blocking, but no test exercises concurrent calls to `identifier()` and `globalIdentifier()` simultaneously.

**Files:**
- Modify: `id/src/test/java/dk/ceti/jdentifiers/id/KSortableIDGeneratorTest.java`

- [ ] **Step 1: Write the test**

Add in the thread safety section:

```java
@Test
void concurrent_mixed_type_generation() throws Exception {
    KSortableIDGenerator gen = new KSortableIDGenerator();
    int idsPerThread = 5_000;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(3);

    List<Long> idResults = new ArrayList<>();
    List<UUID> gidResults = new ArrayList<>();
    List<Integer> lidResults = new ArrayList<>();

    new Thread(() -> {
        try {
            startLatch.await();
            for (int i = 0; i < idsPerThread; i++) {
                idResults.add(gen.<A>identifier().asLong());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            doneLatch.countDown();
        }
    }).start();

    new Thread(() -> {
        try {
            startLatch.await();
            for (int i = 0; i < idsPerThread; i++) {
                gidResults.add(gen.<A>globalIdentifier().asUUID());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            doneLatch.countDown();
        }
    }).start();

    new Thread(() -> {
        try {
            startLatch.await();
            for (int i = 0; i < idsPerThread; i++) {
                lidResults.add(gen.<A>localIdentifier().toInteger());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            doneLatch.countDown();
        }
    }).start();

    startLatch.countDown();
    assertTrue(doneLatch.await(30, TimeUnit.SECONDS));

    // Each type produced the expected count
    assertEquals(idsPerThread, idResults.size());
    assertEquals(idsPerThread, gidResults.size());
    assertEquals(idsPerThread, lidResults.size());

    // Each type's results are unique within their type
    assertEquals(idsPerThread, Set.copyOf(idResults).size(), "All IDs must be unique");
    assertEquals(idsPerThread, Set.copyOf(gidResults).size(), "All GIDs must be unique");
    assertEquals(idsPerThread, Set.copyOf(lidResults).size(), "All LIDs must be unique");
}
```

Note: `Collections.synchronizedList` is not needed because each list is written by a single thread.

- [ ] **Step 2: Run test to verify**

Run: `mvn -pl id test -Dtest=KSortableIDGeneratorTest#concurrent_mixed_type_generation`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add id/src/test/java/dk/ceti/jdentifiers/id/KSortableIDGeneratorTest.java
git commit -m "test: add mixed-type concurrent generation test

Exercises the separate-lock design where identifier(), globalIdentifier(),
and localIdentifier() run concurrently from different threads."
```

---

## Chunk 2: Production Code Hardening (F5, F9, F10, F11, F12, F13, F14, F16)

### Task 4: Cap `maxSpinNanos` and add UUIDv7 named constants (F5, F9, F10, F14, F16)

**Problem:** Multiple constants-related findings in `KSortableIDGenerator`:
- F5: `maxSpinNanos` accepts `Long.MAX_VALUE` → infinite lock hold
- F9/F10: GID MSB/LSB assembly uses raw hex magic numbers
- F14: `GID_COUNTER_MAX` uses hex literal while `LID_COUNTER_MAX` derives from bits
- F16: `GID_COUNTER_INIT_BOUND` has no explanation

**Files:**
- Modify: `id/src/main/java/dk/ceti/jdentifiers/id/KSortableIDGenerator.java`

- [ ] **Step 1: Add UUIDv7 layout constants and GID counter bits**

After the existing constants block (around line 50), add:

```java
static final int GID_COUNTER_BITS = 12;
```

Change `GID_COUNTER_MAX` to derive from `GID_COUNTER_BITS` (like `LID_COUNTER_MAX`):
```java
// BEFORE:
static final int GID_COUNTER_MAX = 0xFFF; // 4095

// AFTER:
static final int GID_COUNTER_MAX = (1 << GID_COUNTER_BITS) - 1; // 4095
```

Add UUIDv7 MSB layout constants (private, not part of public API):
```java
// UUIDv7 MSB layout: [48-bit timestamp | 4-bit version | 12-bit counter]
private static final int UUIDV7_VERSION = 7;
private static final int UUIDV7_VERSION_SHIFT = 12;  // version starts at bit 12 of MSB
private static final int UUIDV7_TIMESTAMP_SHIFT = 16; // timestamp starts at bit 16 of MSB

// UUIDv7 LSB layout: [2-bit variant (10) | 62-bit random]
private static final long UUIDV7_VARIANT_BITS = 0x8000000000000000L; // variant 10
private static final long UUIDV7_RANDOM_MASK = 0x3FFFFFFFFFFFFFFFL;  // 62-bit random
```

Add comment to `GID_COUNTER_INIT_BOUND`:
```java
// BEFORE:
private static final int GID_COUNTER_INIT_BOUND = 1 << 8; // 256

// AFTER:
/**
 * Upper bound (exclusive) for random counter initialization on new-ms transitions.
 * Per RFC 9562 Section 6.2 Method 1: a small random offset prevents counter
 * collisions between independent generators sharing the same millisecond tick.
 * 256 (8 bits) wastes ~3% of counter space on average — acceptable trade-off.
 */
private static final int GID_COUNTER_INIT_BOUND = 1 << 8; // 256
```

- [ ] **Step 2: Update GID assembly to use named constants**

In `globalIdentifier()`, replace:
```java
// BEFORE (line 204):
long randomLsb = (random.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

// AFTER:
long randomLsb = (random.nextLong() & UUIDV7_RANDOM_MASK) | UUIDV7_VARIANT_BITS;
```

```java
// BEFORE (line 249):
long msb = (now << 16) | (0x7L << 12) | (gidCounter & 0xFFF);

// AFTER:
long msb = (now << UUIDV7_TIMESTAMP_SHIFT)
         | ((long) UUIDV7_VERSION << UUIDV7_VERSION_SHIFT)
         | (gidCounter & GID_COUNTER_MAX);
```

- [ ] **Step 3: Add `maxSpinNanos` upper bound**

In the `Builder.maxSpinNanos()` method, add an upper bound check:

```java
// BEFORE:
Builder maxSpinNanos(long maxSpinNanos) {
    if (maxSpinNanos <= 0) {
        throw new IllegalArgumentException("maxSpinNanos must be positive, got " + maxSpinNanos);
    }
    this.maxSpinNanos = maxSpinNanos;
    return this;
}

// AFTER:
Builder maxSpinNanos(long maxSpinNanos) {
    if (maxSpinNanos <= 0) {
        throw new IllegalArgumentException("maxSpinNanos must be positive, got " + maxSpinNanos);
    }
    if (maxSpinNanos > MAX_SPIN_NANOS) {
        throw new IllegalArgumentException(
            "maxSpinNanos must not exceed " + MAX_SPIN_NANOS + " (2s), got " + maxSpinNanos);
    }
    this.maxSpinNanos = maxSpinNanos;
    return this;
}
```

- [ ] **Step 4: Run all tests to verify no regressions**

Run: `mvn -pl id test`
Expected: All tests PASS

- [ ] **Step 5: Verify benchmarks compile**

Run: `mvn -pl id,benchmarks package -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add id/src/main/java/dk/ceti/jdentifiers/id/KSortableIDGenerator.java
git commit -m "refactor: extract UUIDv7 layout constants, cap maxSpinNanos

- GID_COUNTER_BITS constant, GID_COUNTER_MAX now derived from it
- Named constants for UUIDv7 version, variant, shifts, masks
- maxSpinNanos rejects values > MAX_SPIN_NANOS (2s)
- Added RFC 9562 rationale comment on GID_COUNTER_INIT_BOUND"
```

---

### Task 5: Add `toString()` to `KSortableIDGenerator` (F11)

**Problem:** Logging a generator instance shows `KSortableIDGenerator@hash` — no configuration visible for production diagnostics.

**Files:**
- Modify: `id/src/main/java/dk/ceti/jdentifiers/id/KSortableIDGenerator.java`
- Modify: `id/src/test/java/dk/ceti/jdentifiers/id/KSortableIDGeneratorTest.java`

- [ ] **Step 1: Write the test**

```java
@Test
void toString_includes_configuration() {
    KSortableIDGenerator gen = KSortableIDGenerator.builder()
        .nodeBits(10)
        .nodeId(42)
        .build();

    String str = gen.toString();
    assertTrue(str.contains("nodeBits=10"), str);
    assertTrue(str.contains("nodeId=42"), str);
    assertTrue(str.contains("counterBits=12"), str);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl id test -Dtest=KSortableIDGeneratorTest#toString_includes_configuration`
Expected: FAIL (default `Object.toString()`)

- [ ] **Step 3: Implement `toString()`**

Add to `KSortableIDGenerator`, after the `lidEpochMs()` accessor:

```java
@Override
public String toString() {
    return "KSortableIDGenerator{nodeBits=" + nodeBits
        + ", counterBits=" + counterBits
        + ", nodeId=" + nodeId
        + ", lidEpochMs=" + lidEpochMs + '}';
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl id test -Dtest=KSortableIDGeneratorTest#toString_includes_configuration`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add id/src/main/java/dk/ceti/jdentifiers/id/KSortableIDGenerator.java \
        id/src/test/java/dk/ceti/jdentifiers/id/KSortableIDGeneratorTest.java
git commit -m "feat: add toString() to KSortableIDGenerator for diagnostics"
```

---

### Task 6: Fix hardcoded `48` in `KSortableIDDebugger` (F12)

**Problem:** `KSortableIDDebugger.debug(GID)` line 103 hardcodes `48` in the output string instead of using `GID_TIMESTAMP_BITS`.

**Files:**
- Modify: `id/src/main/java/dk/ceti/jdentifiers/id/KSortableIDDebugger.java:103`

- [ ] **Step 1: Replace hardcoded value**

```java
// BEFORE:
sb.append("├── Timestamp: 48 bits = ").append(unixMs)

// AFTER:
sb.append("├── Timestamp: ").append(KSortableIDGenerator.GID_TIMESTAMP_BITS).append(" bits = ").append(unixMs)
```

- [ ] **Step 2: Run debugger tests to verify**

Run: `mvn -pl id test -Dtest=KSortableIDDebuggerTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add id/src/main/java/dk/ceti/jdentifiers/id/KSortableIDDebugger.java
git commit -m "fix: use GID_TIMESTAMP_BITS constant in debugger output"
```

---

### Task 7: Rename mutable state fields for clarity (F13)

**Problem:** `lastIdTimestamp` stores epoch-relative ms while `lastGidTimestamp` stores absolute Unix ms. The identical naming prefix `last*Timestamp` suggests the same reference frame, which could mislead a maintainer.

**Files:**
- Modify: `id/src/main/java/dk/ceti/jdentifiers/id/KSortableIDGenerator.java`

- [ ] **Step 1: Rename fields**

```java
// BEFORE:
private long lastIdTimestamp = -1;
...
private long lastGidTimestamp = -1;

// AFTER (with updated comment):
// ID (64-bit) mutable state — guarded by idLock
// Epoch-relative ms (now - DEFAULT_EPOCH_MS), matching the bit layout
private long lastIdRelativeMs = -1;
private int idCounter;

// GID (128-bit) mutable state — guarded by gidLock
// Absolute Unix ms, matching UUIDv7 timestamp semantics
private long lastGidUnixMs = -1;
private int gidCounter;
```

Use replace-all within the file:
- `lastIdTimestamp` → `lastIdRelativeMs` (7 occurrences: 1 declaration + 6 in `identifier()`)
- `lastGidTimestamp` → `lastGidUnixMs` (7 occurrences: 1 declaration + 6 in `globalIdentifier()`)

- [ ] **Step 2: Run all tests**

Run: `mvn -pl id test`
Expected: All PASS

- [ ] **Step 3: Commit**

```bash
git add id/src/main/java/dk/ceti/jdentifiers/id/KSortableIDGenerator.java
git commit -m "refactor: rename mutable state fields to clarify reference frame

lastIdTimestamp → lastIdRelativeMs (epoch-relative, matches ID bit layout)
lastGidTimestamp → lastGidUnixMs (absolute Unix ms, matches UUIDv7 spec)"
```

---

## Chunk 3: Hybrid Spin-Wait (F3) and Builder Validation Test (F5)

### Task 8: Hybrid spin-then-park for long waits (F3)

**Problem:** The pure `Thread.onSpinWait()` loop is appropriate for counter-overflow recovery (next ms tick, ≤1ms). But for clock regression recovery, the generator could spin for up to 1 second, burning a full CPU core.

**Fix:** After a short spin phase (1ms), switch to `LockSupport.parkNanos()` with 1ms granularity. This preserves low-latency response to the next ms tick while being scheduler-friendly for longer waits.

**Important:** The park happens *inside* the `synchronized` block. This is intentional and correct — `LockSupport.parkNanos` does NOT release the monitor (unlike `Object.wait()`). Other threads still block on the monitor. The improvement is CPU utilization (parked thread doesn't burn CPU), not throughput.

**Files:**
- Modify: `id/src/main/java/dk/ceti/jdentifiers/id/KSortableIDGenerator.java`

- [ ] **Step 1: Add spin-phase constant**

After `MAX_SPIN_NANOS`:
```java
/**
 * Duration of the pure-spin phase before switching to park-based waiting.
 * Tuned to the common case: counter overflow resolves at the next ms tick.
 */
private static final long SPIN_PHASE_NANOS = 1_000_000L; // 1ms
```

Add import:
```java
import java.util.concurrent.locks.LockSupport;
```

- [ ] **Step 2: Extract a private spin-wait helper**

The three spin-wait sites (counter overflow, clock regression for ID; counter overflow, clock regression for GID) all share the same structure. Extract:

```java
/**
 * Blocks until {@code condition} returns a non-null value or the deadline is exceeded.
 * Uses a hybrid strategy: pure spin for the first {@link #SPIN_PHASE_NANOS}, then
 * 1ms park intervals. Runs inside the caller's synchronized block — the monitor
 * is NOT released during park (unlike Object.wait).
 *
 * @param <R>      return type (the new timestamp or clock reading)
 * @param deadline absolute nanoTime deadline
 * @param poller   called on each iteration; returns non-null to exit, null to continue
 * @return the non-null value from the poller
 * @throws IllegalStateException if deadline exceeded
 */
private <R> R spinWait(long deadline, java.util.function.Supplier<R> poller) {
    long spinUntil = System.nanoTime() + SPIN_PHASE_NANOS;
    while (true) {
        R result = poller.get();
        if (result != null) {
            return result;
        }
        long now = System.nanoTime();
        if (now - deadline >= 0) {
            throw new IllegalStateException("Clock did not advance within timeout");
        }
        if (now - spinUntil >= 0) {
            // Past spin phase — park for 1ms to avoid burning CPU
            LockSupport.parkNanos(1_000_000L);
        } else {
            Thread.onSpinWait();
        }
    }
}
```

**WAIT — reconsider this.** A `Supplier<R>` approach introduces allocation of a lambda capture on each call. For a hot path inside a synchronized block, this is undesirable. The lambda would capture `clock`, `lastIdRelativeMs`, etc. — but those are fields, not locals, so the lambda captures `this` (already available, no allocation).

Actually, on modern JVMs (JDK 17+), non-capturing and `this`-capturing lambdas are allocated once and cached. But the `Supplier<R>` approach also requires boxing the `Long` return. This is the hot path.

**Better approach:** Inline the hybrid logic into each spin-wait site. It's 3 lines of change per site (add `spinUntil` local, check before `onSpinWait`, add `parkNanos` branch). This avoids abstraction overhead.

- [ ] **Step 2 (revised): Inline hybrid spin at each spin-wait site**

There are 4 spin-wait sites total:
1. `identifier()` counter overflow (line ~147)
2. `identifier()` clock regression (line ~164)
3. `globalIdentifier()` counter overflow (line ~215)
4. `globalIdentifier()` clock regression (line ~231)

At each site, transform:
```java
// BEFORE:
long deadline = System.nanoTime() + maxSpinNanos;
while (conditionNotMet) {
    Thread.onSpinWait();
    if (System.nanoTime() - deadline >= 0) {
        throw new IllegalStateException("Clock did not advance within timeout");
    }
}

// AFTER:
long deadline = System.nanoTime() + maxSpinNanos;
long spinUntil = System.nanoTime() + SPIN_PHASE_NANOS;
while (conditionNotMet) {
    if (System.nanoTime() - spinUntil >= 0) {
        LockSupport.parkNanos(1_000_000L);
    } else {
        Thread.onSpinWait();
    }
    if (System.nanoTime() - deadline >= 0) {
        throw new IllegalStateException("Clock did not advance within timeout");
    }
}
```

- [ ] **Step 3: Run all tests**

Run: `mvn -pl id test`
Expected: All PASS. The `maxSpinNanos(50_000_000L)` tests should still work — 50ms > 1ms spin phase, so the park branch is exercised.

- [ ] **Step 4: Verify benchmarks compile**

Run: `mvn -pl id,benchmarks package -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add id/src/main/java/dk/ceti/jdentifiers/id/KSortableIDGenerator.java
git commit -m "perf: hybrid spin-then-park for spin-wait loops

Pure Thread.onSpinWait() for the first 1ms (covers counter-overflow
recovery at next ms tick), then LockSupport.parkNanos(1ms) for longer
waits (clock regression). Reduces CPU waste from spinning a full core
for up to 2 seconds."
```

---

### Task 9: Test for `maxSpinNanos` upper bound (F5)

**Files:**
- Modify: `id/src/test/java/dk/ceti/jdentifiers/id/KSortableIDGeneratorTest.java`

- [ ] **Step 1: Update existing test to also check upper bound**

Modify `builder_rejects_non_positive_max_spin_nanos`:

```java
// Rename the test method to reflect broader scope:
@Test
void builder_rejects_invalid_max_spin_nanos() {
    // Zero
    assertThrows(IllegalArgumentException.class, () ->
        KSortableIDGenerator.builder().maxSpinNanos(0));
    // Negative
    assertThrows(IllegalArgumentException.class, () ->
        KSortableIDGenerator.builder().maxSpinNanos(-1));
    // Exceeds MAX_SPIN_NANOS
    assertThrows(IllegalArgumentException.class, () ->
        KSortableIDGenerator.builder().maxSpinNanos(KSortableIDGenerator.MAX_SPIN_NANOS + 1));
}
```

- [ ] **Step 2: Run test**

Run: `mvn -pl id test -Dtest=KSortableIDGeneratorTest#builder_rejects_invalid_max_spin_nanos`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add id/src/test/java/dk/ceti/jdentifiers/id/KSortableIDGeneratorTest.java
git commit -m "test: verify maxSpinNanos upper bound validation"
```

---

## Chunk 4: Final Verification

### Task 10: Full verification pass

- [ ] **Step 1: Run all tests**

Run: `mvn -pl id test`
Expected: All PASS

- [ ] **Step 2: Verify benchmarks compile**

Run: `mvn -pl id,benchmarks package -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Verify the formerly-slow test is now fast**

Run: `mvn -pl id test -Dtest=KSortableIDGeneratorTest#gid_timestamp_overflow_after_spin_wait`
Expected: PASS in <1s (was 2.06s)

- [ ] **Step 4: Eyeball diff for accidental changes**

Run: `git diff --stat HEAD~N` (where N = number of commits in this plan)
Verify only expected files changed.

---

## Deferred to Future Plans

These findings are real but require either a separate design discussion or have cross-cutting implications that warrant their own plan:

| Finding | Why deferred | Prerequisite |
|---------|-------------|--------------|
| F2: Lock held during spin-wait | Changing to lock-free or lock-release-during-wait requires fundamental redesign of the state machine. Current bounded timeout (2s max) is acceptable. | Architecture review |
| F6: False sharing | Requires `@Contended` (JDK internal) or manual padding. Measure first with JMH multi-threaded benchmark to confirm impact. | Benchmark data |
| F7: `random.nextInt()` inside gidLock | Could pre-compute outside lock, but adds complexity and the cost is ~200ns/ms-transition. | Benchmark data |
| F8: Static shared PRNG in RandomIDGenerator | Changing to per-instance changes memory footprint and threading contract. Needs benchmark comparison. | Separate plan |
