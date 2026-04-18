package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacAddressNodeIdTest {

    private static final byte[] TEST_MAC =
        {0x00, 0x1A, 0x2B, 0x3C, 0x4D, 0x5E};

    // 0x3C4D5E & 0x3FF = 350, & 0x1F = 30, & 0xFFFF = 19806

    @Test
    void macAddress_masks_known_bytes_to_10_bits() {
        var supplier = NodeIdStrategies.macAddress(10, TEST_MAC);
        assertEquals(10, supplier.nodeBits());
        assertEquals(350, supplier.nodeId());
    }

    @Test
    void macAddress_masks_known_bytes_to_5_bits() {
        assertEquals(30, NodeIdStrategies.macAddress(5, TEST_MAC).nodeId());
    }

    @Test
    void macAddress_masks_known_bytes_to_16_bits() {
        assertEquals(19806, NodeIdStrategies.macAddress(16, TEST_MAC).nodeId());
    }

    @Test
    void macAddress_is_deterministic() {
        assertEquals(
            NodeIdStrategies.macAddress(10, TEST_MAC).nodeId(),
            NodeIdStrategies.macAddress(10, TEST_MAC).nodeId()
        );
    }

    @Test
    void macAddress_rejects_null_hardware_address() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.macAddress(10, (byte[]) null)
        );
    }

    @Test
    void macAddress_rejects_short_hardware_address() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.macAddress(10, new byte[]{0x01})
        );
    }

    @Test
    void macAddress_rejects_invalid_node_bits() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.macAddress(0)
        );
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.macAddress(22)
        );
    }

    @Test
    void macAddress_public_returns_value_within_range() {
        try {
            var supplier = NodeIdStrategies.macAddress(10);
            assertEquals(10, supplier.nodeBits());
            assertTrue(supplier.nodeId() >= 0 && supplier.nodeId() < 1024);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("network interface"));
        }
    }
}
