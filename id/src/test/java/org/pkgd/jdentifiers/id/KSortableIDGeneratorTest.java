package org.pkgd.jdentifiers.id;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KSortableIDGeneratorTest {
    private static IDGenerator generator;

    @BeforeAll
    static void setupSpec() {
        generator = new KSortableIDGenerator();
    }

    @Test
    void should_be_generated_within_same_microsecond() {
        final var id = ID.fromLong(7617621813640145527L);
        assertEquals("6kdstnqxmkakq", id.toString(), () -> KSortableDebugger.asStrong(id));
    }

    @Test
    void generated_should_be_with_same_time() {
        final var id = generator.identifier();
        assertTrue(false, () -> KSortableDebugger.asStrong(id));
    }
}
