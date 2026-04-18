package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomNodeIdTest {

    @Test
    void random_returns_value_within_range() {
        var supplier = NodeIdStrategies.random(10);
        assertEquals(10, supplier.nodeBits());
        assertTrue(supplier.nodeId() >= 0 && supplier.nodeId() < 1024);
    }

    @Test
    void random_respects_various_bit_widths() {
        for (var bits : new int[]{1, 5, 10, 21}) {
            var max = 1 << bits;
            for (var i = 0; i < 100; i++) {
                var nodeId = NodeIdStrategies.random(bits).nodeId();
                assertTrue(nodeId >= 0 && nodeId < max,
                    "bits=" + bits + " nodeId=" + nodeId
                        + " not in [0, " + max + ")"
                );
            }
        }
    }

    @Test
    void random_rejects_invalid_node_bits() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.random(0)
        );
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.random(22)
        );
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.random(-1)
        );
    }
}
