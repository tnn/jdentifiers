package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HostnameNodeIdTest {

    @Test
    void hostname_uses_sha256_for_all_hostnames() {
        assertEquals(901,
            NodeIdStrategies.hostname(10, "myapp-7f8b9c6d4-x2k9p").nodeId()
        );
    }

    @Test
    void hostname_sha256_for_statefulset_like_names() {
        assertEquals(710,
            NodeIdStrategies.hostname(10, "web-0").nodeId()
        );
        assertEquals(762,
            NodeIdStrategies.hostname(10, "web-1").nodeId()
        );
    }

    @Test
    void hostname_no_false_positives_for_aws_dns() {
        assertEquals(666,
            NodeIdStrategies.hostname(10, "ip-172-31-42-7").nodeId()
        );
    }

    @Test
    void hostname_no_false_positives_for_port_in_name() {
        assertEquals(910,
            NodeIdStrategies.hostname(10, "redis-sentinel-26379").nodeId()
        );
    }

    @Test
    void hostname_no_false_positives_for_date_stamp() {
        assertEquals(1010,
            NodeIdStrategies.hostname(10, "build-20260418").nodeId()
        );
    }

    @Test
    void hostname_is_deterministic() {
        var a = NodeIdStrategies.hostname(10, "plain-host");
        var b = NodeIdStrategies.hostname(10, "plain-host");
        assertEquals(a.nodeId(), b.nodeId());
    }

    @Test
    void hostname_different_hosts_differ() {
        assertNotEquals(
            NodeIdStrategies.hostname(10, "host-a").nodeId(),
            NodeIdStrategies.hostname(10, "host-b").nodeId()
        );
    }

    @Test
    void hostname_throws_when_hostname_null() {
        assertThrows(IllegalStateException.class,
            () -> NodeIdStrategies.hostname(10, (String) null)
        );
    }

    @Test
    void hostname_rejects_invalid_node_bits() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.hostname(0)
        );
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.hostname(22)
        );
    }
}
