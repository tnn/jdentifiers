package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoNodeIdTest {

    @Test
    void auto_public_always_succeeds() {
        var supplier = NodeIdStrategies.auto(10);
        assertEquals(10, supplier.nodeBits());
        assertTrue(supplier.nodeId() >= 0 && supplier.nodeId() < 1024);
    }

    @Test
    void auto_prefers_mac_address() {
        var supplier = NodeIdStrategies.auto(10, TEST_MAC, "some-host");
        assertEquals(350, supplier.nodeId());
        assertTrue(supplier.toString().contains("macAddress"));
    }

    @Test
    void auto_falls_back_to_kubernetes_when_no_mac() {
        var supplier = NodeIdStrategies.auto(10, null,
            "myapp-7f8b9c6d4-x2k9p"
        );
        assertEquals(870, supplier.nodeId());
        assertTrue(supplier.toString().contains("kubernetes"));
    }

    @Test
    void auto_falls_back_to_hostname_sha256_when_no_mac_no_k8s() {
        var supplier = NodeIdStrategies.auto(10, null, "my-server");
        assertEquals(976, supplier.nodeId());
        assertTrue(supplier.toString().contains("hostname"));
    }

    @Test
    void auto_falls_back_to_random_when_nothing_available() {
        var supplier = NodeIdStrategies.auto(10, null, null);
        assertEquals(10, supplier.nodeBits());
        assertTrue(supplier.nodeId() >= 0 && supplier.nodeId() < 1024);
        assertTrue(supplier.toString().contains("random"));
    }

    @Test
    void auto_rejects_invalid_node_bits() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.auto(0)
        );
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.auto(22)
        );
    }

    @Test
    void auto_integrates_with_generator() {
        var gen = KSortableIDGenerator.builder()
            .nodeId(NodeIdStrategies.auto(10))
            .build();
        assertEquals(10, gen.nodeBits());
        assertTrue(gen.nodeId() >= 0 && gen.nodeId() < 1024);
    }

    private static final byte[] TEST_MAC =
        {0x00, 0x1A, 0x2B, 0x3C, 0x4D, 0x5E};

    private interface A extends IDAble {
    }
}
