package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubernetesNodeIdTest {

    // ---- Deployment detection ----

    @Test
    void kubernetes_detects_deployment_and_decodes_pod_suffix() {
        // "myapp-7f8b9c6d4-x2k9p" matches K8s Deployment pattern
        // Pod suffix "x2k9p" decoded base-27: x=18, 2=20, k=7, 9=26, p=11
        // 18*27^4 + 20*27^3 + 7*27^2 + 26*27 + 11 = 9_965_414
        // 9_965_414 & 0x3FF = 870
        var supplier = NodeIdStrategies.kubernetes(10,
            "myapp-7f8b9c6d4-x2k9p"
        );
        assertEquals(10, supplier.nodeBits());
        assertEquals(870, supplier.nodeId());
    }

    @Test
    void kubernetes_deployment_masks_to_different_bit_widths() {
        // Pod suffix "x2k9p" decoded = 9_965_414
        // 9_965_414 & 0x1 = 0
        assertEquals(0,
            NodeIdStrategies.kubernetes(1, "myapp-7f8b9c6d4-x2k9p").nodeId()
        );
        // 9_965_414 & 0x1F = 6
        assertEquals(6,
            NodeIdStrategies.kubernetes(5, "myapp-7f8b9c6d4-x2k9p").nodeId()
        );
    }

    @Test
    void kubernetes_different_app_names_same_suffix_same_node_id() {
        var a = NodeIdStrategies.kubernetes(10,
            "myapp-7f8b9c6d4-x2k9p"
        ).nodeId();
        var b = NodeIdStrategies.kubernetes(10,
            "my-special-app-7f8b9c6d4-x2k9p"
        ).nodeId();
        var c = NodeIdStrategies.kubernetes(10,
            "my-very-special-app1-bcd2fgh5j-x2k9p"
        ).nodeId();
        assertEquals(a, b);
        assertEquals(b, c);
    }

    @Test
    void kubernetes_different_suffixes_differ() {
        var a = NodeIdStrategies.kubernetes(10,
            "myapp-7f8b9c6d4-x2k9p"
        ).nodeId();
        var b = NodeIdStrategies.kubernetes(10,
            "myapp-7f8b9c6d4-bcdfg"
        ).nodeId();
        assertNotEquals(a, b);
    }

    @Test
    void kubernetes_short_rs_hash_still_matches() {
        // RS hash can be as short as 1 char for small FNV values
        var a = NodeIdStrategies.kubernetes(10, "myapp-4-x2k9p").nodeId();
        var b = NodeIdStrategies.kubernetes(10, "myapp-86-x2k9p").nodeId();
        assertEquals(a, b);
    }

    // ---- Fallback and false-positive prevention ----

    @Test
    void kubernetes_rs_hash_with_digit_3_does_not_match() {
        // Digit '3' is NOT in the K8s alphabet
        var supplier = NodeIdStrategies.kubernetes(10,
            "myapp-3abc-x2k9p"
        );
        var sha256Value = NodeIdStrategies.hostname(10,
            "myapp-3abc-x2k9p"
        ).nodeId();
        assertEquals(sha256Value, supplier.nodeId());
    }

    @Test
    void kubernetes_falls_back_to_sha256_for_non_k8s_hostname() {
        var kubeValue = NodeIdStrategies.kubernetes(10, "my-server").nodeId();
        var hostValue = NodeIdStrategies.hostname(10, "my-server").nodeId();
        assertEquals(hostValue, kubeValue);
    }

    @Test
    void kubernetes_no_statefulset_detection() {
        var kubeValue = NodeIdStrategies.kubernetes(10, "web-0").nodeId();
        var sha256Value = NodeIdStrategies.hostname(10, "web-0").nodeId();
        assertEquals(sha256Value, kubeValue);
    }

    @Test
    void kubernetes_no_false_positive_for_aws_dns() {
        var kubeValue = NodeIdStrategies.kubernetes(10,
            "ip-172-31-42-7"
        ).nodeId();
        var sha256Value = NodeIdStrategies.hostname(10,
            "ip-172-31-42-7"
        ).nodeId();
        assertEquals(sha256Value, kubeValue);
    }

    @Test
    void kubernetes_no_false_positive_for_port_in_name() {
        var kubeValue = NodeIdStrategies.kubernetes(10,
            "redis-sentinel-26379"
        ).nodeId();
        var sha256Value = NodeIdStrategies.hostname(10,
            "redis-sentinel-26379"
        ).nodeId();
        assertEquals(sha256Value, kubeValue);
    }

    // ---- Validation ----

    @Test
    void kubernetes_throws_when_hostname_null() {
        assertThrows(IllegalStateException.class,
            () -> NodeIdStrategies.kubernetes(10, (String) null)
        );
    }

    @Test
    void kubernetes_rejects_invalid_node_bits() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.kubernetes(0)
        );
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.kubernetes(22)
        );
    }

    // ---- K8s alphabet / char-class consistency ----

    @Test
    void k8s_char_class_matches_alphabet_exactly() {
        var alphabet = NodeIdStrategies.K8S_ALPHABET;

        for (var i = 0; i < alphabet.length(); i++) {
            var c = alphabet.charAt(i);
            var suffix = "" + c + c + c + c + c;
            var hostname = "app-b-" + suffix;
            assertTrue(NodeIdStrategies.K8S_DEPLOYMENT_PATTERN
                    .matcher(hostname).matches(),
                "Alphabet char '" + c + "' should be matched by "
                    + "K8S_DEPLOYMENT_PATTERN in suffix position"
            );
        }

        for (var c = (char) 0; c < 128; c++) {
            var inAlphabet = alphabet.indexOf(c) >= 0;
            var suffix = "" + c + c + c + c + c;
            var hostname = "app-b-" + suffix;
            var matchesPattern = NodeIdStrategies.K8S_DEPLOYMENT_PATTERN
                .matcher(hostname).matches();
            if (!inAlphabet && matchesPattern) {
                throw new AssertionError(
                    "Char '" + c + "' (0x" + Integer.toHexString(c)
                        + ") is NOT in K8S_ALPHABET but IS matched "
                        + "by K8S_DEPLOYMENT_PATTERN");
            }
        }
    }

    @Test
    void k8s_alphabet_has_27_characters() {
        assertEquals(27, NodeIdStrategies.K8S_ALPHABET.length());
    }

    // ---- decodeK8sSuffix ----

    @Test
    void decodeK8sSuffix_all_zeros() {
        assertEquals(0, NodeIdStrategies.decodeK8sSuffix("bbbbb"));
    }

    @Test
    void decodeK8sSuffix_all_max() {
        assertEquals(14348906, NodeIdStrategies.decodeK8sSuffix("99999"));
    }

    @Test
    void decodeK8sSuffix_known_value() {
        assertEquals(9965414, NodeIdStrategies.decodeK8sSuffix("x2k9p"));
    }

    @Test
    void decodeK8sSuffix_single_char() {
        assertEquals(0, NodeIdStrategies.decodeK8sSuffix("b"));
        assertEquals(26, NodeIdStrategies.decodeK8sSuffix("9"));
    }

    @Test
    void decodeK8sSuffix_rejects_invalid_character() {
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.decodeK8sSuffix("abcde")
        );
        assertThrows(IllegalArgumentException.class,
            () -> NodeIdStrategies.decodeK8sSuffix("3bcdf")
        );
    }
}
