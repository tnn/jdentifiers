package dk.ceti.jdentifiers.id;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * Built-in node ID strategies for {@link KSortableIDGenerator}.
 *
 * @see NodeIdSupplier
 */
public final class NodeIdStrategies {

    /**
     * Kubernetes safe alphabet: consonants + digits 2, 4-9 (27 chars).
     *
     * @see <a href="https://github.com/kubernetes/apimachinery/blob/master/pkg/util/rand/rand.go">
     *     K8s apimachinery rand.go</a>
     */
    static final String K8S_ALPHABET = "bcdfghjklmnpqrstvwxz2456789";

    private static final String K8S_CC = "[bcdfghjklmnpqrstvwxz24-9]";

    /**
     * Matches {@code {name}-{rs-hash}-{pod-suffix}}. Group 1: pod suffix.
     */
    static final Pattern K8S_DEPLOYMENT_PATTERN = Pattern.compile(
        "^.+-" + K8S_CC + "{1,10}-(" + K8S_CC + "{5})$");

    private NodeIdStrategies() {
    }

    /**
     * Cascading auto-detection: MAC address, K8s Deployment suffix,
     * hostname SHA-256, random. Never throws.
     *
     * @param nodeBits number of node bits (1 to 21)
     * @return supplier from the best available source
     */
    public static NodeIdSupplier auto(int nodeBits) {
        validateNodeBits(nodeBits);
        return auto(nodeBits, findHardwareAddressOrNull(),
            resolveHostnameOrNull()
        );
    }

    static NodeIdSupplier auto(int nodeBits, byte[] hardwareAddress,
                               String hostname
    ) {
        if (hardwareAddress != null && hardwareAddress.length >= 2) {
            return macAddress(nodeBits, hardwareAddress);
        }
        if (hostname != null) {
            var mask = (1 << nodeBits) - 1;
            var m = K8S_DEPLOYMENT_PATTERN.matcher(hostname);
            if (m.matches()) {
                return named("kubernetes", nodeBits,
                    decodeK8sSuffix(m.group(1)) & mask
                );
            }
            return named("hostname", nodeBits,
                hashHostname(hostname, nodeBits)
            );
        }
        return random(nodeBits);
    }

    /**
     * Fixed node ID.
     *
     * @param nodeBits number of node bits (1 to 21)
     * @param nodeId   the node identifier, must be in [0, 2^nodeBits)
     * @return supplier with fixed value
     */
    public static NodeIdSupplier of(int nodeBits, int nodeId) {
        return named("of", nodeBits, nodeId);
    }

    private static NodeIdSupplier named(String strategy,
                                        int nodeBits, int nodeId
    ) {
        validateNodeBits(nodeBits);
        var maxNodeId = (1 << nodeBits) - 1;
        if (nodeId < 0 || nodeId > maxNodeId) {
            throw new IllegalArgumentException(
                "nodeId must be in [0, " + maxNodeId + "] for "
                    + nodeBits + " node bits, got " + nodeId);
        }
        return new NodeIdSupplier() {
            @Override
            public int nodeBits() {
                return nodeBits;
            }

            @Override
            public int nodeId() {
                return nodeId;
            }

            @Override
            public String toString() {
                return "NodeIdSupplier." + strategy
                    + "(nodeBits=" + nodeBits
                    + ", nodeId=" + nodeId + ")";
            }
        };
    }

    /**
     * Rightmost bytes of the first non-loopback, active NIC, masked to
     * {@code nodeBits}. Best for bare-metal/VMs; often unavailable in
     * containers.
     *
     * @param nodeBits number of node bits (1 to 21)
     * @return supplier resolved from the local MAC address
     */
    public static NodeIdSupplier macAddress(int nodeBits) {
        validateNodeBits(nodeBits);
        return macAddress(nodeBits, findHardwareAddress());
    }

    static NodeIdSupplier macAddress(int nodeBits, byte[] hardwareAddress) {
        if (hardwareAddress == null) {
            throw new IllegalArgumentException(
                "hardwareAddress must not be null");
        }
        if (hardwareAddress.length < 2) {
            throw new IllegalArgumentException(
                "hardwareAddress must be at least 2 bytes, got "
                    + hardwareAddress.length);
        }
        var packed = packTailBytes(hardwareAddress);
        var maskedId = (int) (packed & ((1L << nodeBits) - 1));
        return named("macAddress", nodeBits, maskedId);
    }

    /**
     * Packs the last 3 (or fewer) bytes of {@code hw} into a long, big-endian.
     */
    private static long packTailBytes(byte[] hw) {
        var n = Math.min(3, hw.length);
        var result = 0L;
        for (var i = hw.length - n; i < hw.length; i++) {
            result = (result << 8) | (hw[i] & 0xFFL);
        }
        return result;
    }

    private static byte[] findHardwareAddressOrNull() {
        try {
            return findHardwareAddress();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private static byte[] findHardwareAddress() {
        try {
            for (var ni
                : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isLoopback() && ni.isUp()) {
                    var hw = ni.getHardwareAddress();
                    if (hw != null && hw.length >= 2) {
                        return hw;
                    }
                }
            }
        } catch (SocketException e) {
            throw new IllegalStateException(
                "Failed to enumerate network interfaces", e);
        }
        throw new IllegalStateException(
            "No suitable network interface found for MAC-based node ID");
    }

    /**
     * Cryptographically random node ID in {@code [0, 2^nodeBits)}.
     *
     * @param nodeBits number of node bits (1 to 21)
     * @return supplier with a random value
     */
    public static NodeIdSupplier random(int nodeBits) {
        validateNodeBits(nodeBits);
        var value = SecureRandoms.create().nextInt(1 << nodeBits);
        return named("random", nodeBits, value);
    }

    /**
     * SHA-256 of the local hostname, first 4 bytes masked to {@code nodeBits}.
     * No heuristics. For K8s-aware parsing, use {@link #kubernetes(int)}.
     *
     * <p>Reads {@code HOSTNAME} env var, then {@code InetAddress.getLocalHost()}.
     *
     * @param nodeBits number of node bits (1 to 21)
     * @return supplier resolved from the hostname
     */
    public static NodeIdSupplier hostname(int nodeBits) {
        validateNodeBits(nodeBits);
        return hostname(nodeBits, resolveHostname());
    }

    static NodeIdSupplier hostname(int nodeBits, String hostname) {
        validateNodeBits(nodeBits);
        if (hostname == null) {
            throw new IllegalStateException(
                "Unable to determine hostname for node ID");
        }
        return named("hostname", nodeBits, hashHostname(hostname, nodeBits));
    }

    /**
     * K8s Deployment-aware strategy. If the hostname matches
     * {@code {name}-{rs-hash}-{pod-suffix}} (using the K8s safe alphabet),
     * the 5-char pod suffix is decoded as a base-27 integer (~23.8 bits
     * of entropy). Non-matching hostnames fall back to SHA-256.
     *
     * <p>StatefulSets are not detected (the {@code {name}-{ordinal}} pattern
     * has too many false positives). Use {@link #of(int, int)} with the
     * ordinal from a downward API env var.
     *
     * <p>Reads {@code HOSTNAME} env var, then {@code InetAddress.getLocalHost()}.
     *
     * @param nodeBits number of node bits (1 to 21)
     * @return supplier resolved from the hostname
     */
    public static NodeIdSupplier kubernetes(int nodeBits) {
        validateNodeBits(nodeBits);
        return kubernetes(nodeBits, resolveHostname());
    }

    static NodeIdSupplier kubernetes(int nodeBits, String hostname) {
        validateNodeBits(nodeBits);
        if (hostname == null) {
            throw new IllegalStateException(
                "Unable to determine hostname for node ID");
        }
        var mask = (1 << nodeBits) - 1;
        var m = K8S_DEPLOYMENT_PATTERN.matcher(hostname);
        if (m.matches()) {
            return named("kubernetes", nodeBits,
                decodeK8sSuffix(m.group(1)) & mask
            );
        }
        return named("kubernetes", nodeBits,
            hashHostname(hostname, nodeBits)
        );
    }

    /**
     * Decodes a K8s pod suffix as a base-27 integer.
     */
    static int decodeK8sSuffix(String suffix) {
        var value = 0;
        for (var i = 0; i < suffix.length(); i++) {
            var index = K8S_ALPHABET.indexOf(suffix.charAt(i));
            if (index < 0) {
                throw new IllegalArgumentException(
                    "Character '" + suffix.charAt(i)
                        + "' not in K8s alphabet");
            }
            value = value * K8S_ALPHABET.length() + index;
        }
        return value;
    }

    private static String resolveHostnameOrNull() {
        try {
            return resolveHostname();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private static String resolveHostname() {
        var hostname = System.getenv("HOSTNAME");
        if (hostname != null) {
            return hostname;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(
                "Unable to determine hostname for node ID", e);
        }
    }

    private static int hashHostname(String hostname, int nodeBits) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                .digest(hostname.getBytes(StandardCharsets.UTF_8));
            var value = ((digest[0] & 0xFF) << 24)
                | ((digest[1] & 0xFF) << 16)
                | ((digest[2] & 0xFF) << 8)
                | (digest[3] & 0xFF);
            // value may be negative (bit 31 set), but the mask only
            // keeps the low nodeBits (max 21), so the sign is irrelevant.
            return value & ((1 << nodeBits) - 1);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 not available", e);
        }
    }

    private static void validateNodeBits(int nodeBits) {
        if (nodeBits < 1
            || nodeBits > KSortableIDGenerator.ID_PAYLOAD_BITS - 1) {
            throw new IllegalArgumentException(
                "nodeBits must be in [1, "
                    + (KSortableIDGenerator.ID_PAYLOAD_BITS - 1)
                    + "], got " + nodeBits);
        }
    }
}
