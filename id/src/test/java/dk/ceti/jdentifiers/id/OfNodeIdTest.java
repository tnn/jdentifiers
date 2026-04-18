package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OfNodeIdTest {

    @Test
    void of_returns_fixed_values() {
        var supplier = NodeIdStrategies.of(10, 42);
        assertEquals(10, supplier.nodeBits());
        assertEquals(42, supplier.nodeId());
    }

    @Test
    void of_accepts_boundary_values() {
        var min = NodeIdStrategies.of(1, 0);
        assertEquals(1, min.nodeBits());
        assertEquals(0, min.nodeId());

        assertEquals(1, NodeIdStrategies.of(1, 1).nodeId());
        assertEquals(1023, NodeIdStrategies.of(10, 1023).nodeId());
        assertEquals(21, NodeIdStrategies.of(21, 0).nodeBits());
    }

    @Test
    void of_rejects_node_id_exceeding_range() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.of(10, 1024)
        );
    }

    @Test
    void of_rejects_negative_node_id() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.of(10, -1)
        );
    }

    @Test
    void of_rejects_zero_node_bits() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.of(0, 0)
        );
    }

    @Test
    void of_rejects_node_bits_exceeding_21() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.of(22, 0)
        );
    }

    @Test
    void of_rejects_negative_node_bits() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.of(-1, 0)
        );
    }
}
