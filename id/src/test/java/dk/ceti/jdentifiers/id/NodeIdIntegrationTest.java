package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeIdIntegrationTest {

    private static final byte[] TEST_MAC =
        {0x00, 0x1A, 0x2B, 0x3C, 0x4D, 0x5E};

    @Test
    void of_integrates_with_generator() {
        var gen = KSortableIDGenerator.builder()
            .nodeId(NodeIdStrategies.of(10, 42))
            .build();

        assertEquals(10, gen.nodeBits());
        assertEquals(42, gen.nodeId());
        assertEquals(42, extractNode(gen.identifier()));
    }

    @Test
    void random_integrates_with_generator() {
        var gen = KSortableIDGenerator.builder()
            .nodeId(NodeIdStrategies.random(10))
            .build();

        assertEquals(10, gen.nodeBits());
        var nodeId = gen.nodeId();
        assertTrue(nodeId >= 0 && nodeId < 1024);

        for (var i = 0; i < 100; i++) {
            assertEquals(nodeId, extractNode(gen.identifier()));
        }
    }

    @Test
    void hostname_integrates_with_generator() {
        var gen = KSortableIDGenerator.builder()
            .nodeId(NodeIdStrategies.hostname(10, "test-host"))
            .build();

        assertEquals(10, gen.nodeBits());
        assertEquals(643, gen.nodeId());
    }

    @Test
    void kubernetes_integrates_with_generator() {
        var gen = KSortableIDGenerator.builder()
            .nodeId(NodeIdStrategies.kubernetes(10,
                "myapp-7f8b9c6d4-x2k9p"))
            .build();

        assertEquals(10, gen.nodeBits());
        assertEquals(870, gen.nodeId());
        assertEquals(870, extractNode(gen.identifier()));
    }

    @Test
    void macAddress_integrates_with_generator() {
        var gen = KSortableIDGenerator.builder()
            .nodeId(NodeIdStrategies.macAddress(10, TEST_MAC))
            .build();

        assertEquals(10, gen.nodeBits());
        assertEquals(350, gen.nodeId());
        assertEquals(350, extractNode(gen.identifier()));
    }

    // ---- toString() ----

    @Test
    void of_toString_includes_strategy_and_values() {
        var s = NodeIdStrategies.of(10, 42).toString();
        assertTrue(s.contains("of"), s);
        assertTrue(s.contains("10"), s);
        assertTrue(s.contains("42"), s);
    }

    @Test
    void random_toString_includes_strategy() {
        var s = NodeIdStrategies.random(10).toString();
        assertTrue(s.contains("random"), s);
    }

    @Test
    void hostname_toString_includes_strategy() {
        var s = NodeIdStrategies.hostname(10, "test-host").toString();
        assertTrue(s.contains("hostname"), s);
    }

    @Test
    void kubernetes_toString_includes_strategy() {
        var s = NodeIdStrategies.kubernetes(10,
            "myapp-7f8b9c6d4-x2k9p").toString();
        assertTrue(s.contains("kubernetes"), s);
    }

    @Test
    void macAddress_toString_includes_strategy() {
        var s = NodeIdStrategies.macAddress(10, TEST_MAC).toString();
        assertTrue(s.contains("macAddress"), s);
    }

    private static int extractNode(ID<?> id) {
        return (int) ((id.asLong() >>> 12) & 0x3FF);
    }

    private interface A extends IDAble {
    }
}
