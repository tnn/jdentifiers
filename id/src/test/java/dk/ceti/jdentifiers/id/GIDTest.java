package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GIDTest {

    private static final UUID UUID_A = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID UUID_B = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

    @Test
    void fromString_round_trip() {
        final GID<User> gid = GID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(UUID_A, gid.asUUID());
    }

    @Test
    void fromUuid_round_trip() {
        final GID<User> gid = GID.fromUuid(UUID_A);
        assertEquals(UUID_A, gid.asUUID());
    }

    @Test
    void equals_same_uuid() {
        assertEquals(GID.fromUuid(UUID_A), GID.fromUuid(UUID_A));
    }

    @Test
    void equals_different_uuid() {
        assertNotEquals(GID.fromUuid(UUID_A), GID.fromUuid(UUID_B));
    }

    @Test
    void equals_null() {
        assertFalse(GID.fromUuid(UUID_A).equals(null));
    }

    @Test
    void equals_different_type() {
        assertFalse(GID.fromUuid(UUID_A).equals(UUID_A));
    }

    @Test
    void hashCode_same_uuid() {
        assertEquals(GID.fromUuid(UUID_A).hashCode(), GID.fromUuid(UUID_A).hashCode());
    }

    @Test
    void compareTo_less_than() {
        final GID<User> a = GID.fromUuid(UUID_A);
        final GID<User> b = GID.fromUuid(UUID_B);
        assertTrue(a.compareTo(b) < 0);
    }

    @Test
    void compareTo_equal() {
        final GID<User> a = GID.fromUuid(UUID_A);
        final GID<User> b = GID.fromUuid(UUID_A);
        assertEquals(0, a.compareTo(b));
    }

    @Test
    void compareTo_greater_than() {
        final GID<User> a = GID.fromUuid(UUID_A);
        final GID<User> b = GID.fromUuid(UUID_B);
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    void compareTo_unsigned_ordering() {
        // UUID with MSB sign bit set — signed comparison would sort this before low
        final GID<User> high = GID.fromString("f0000000-0000-0000-0000-000000000000");
        final GID<User> low = GID.fromString("00000000-0000-0000-0000-000000000000");
        assertTrue(high.compareTo(low) > 0, "unsigned: f... should sort after 0...");
    }

    @Test
    void toString_returns_uuid_string() {
        assertEquals("550e8400-e29b-41d4-a716-446655440000", GID.fromUuid(UUID_A).toString());
    }

    @Test
    void cast_preserves_value() {
        final GID<User> user = GID.fromUuid(UUID_A);
        final GID<Organization> org = GID.cast(user);
        assertEquals(user, org);
    }

    @Test
    void fromString_invalid_input() {
        assertThrows(IllegalArgumentException.class, () -> GID.fromString("not-a-uuid"));
    }

    @Test
    void fromString_empty_input() {
        assertThrows(IllegalArgumentException.class, () -> GID.fromString(""));
    }

    @Test
    void fromString_null_input() {
        var ex = assertThrows(NullPointerException.class, () -> GID.fromString(null));
        assertEquals("gidStr must not be null", ex.getMessage());
    }

    @Test
    void fromUuid_null_input() {
        var ex = assertThrows(NullPointerException.class, () -> GID.fromUuid(null));
        assertEquals("uuid must not be null", ex.getMessage());
    }

    @Test
    void java_serialization_round_trip() throws Exception {
        final GID<User> original = GID.fromUuid(UUID_A);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(original);
        GID<?> deserialized = (GID<?>) new ObjectInputStream(
            new ByteArrayInputStream(baos.toByteArray())).readObject();
        assertEquals(original, deserialized);
    }

    @Test
    void compareTo_differs_from_uuid_signed_comparison() {
        // UUID with MSB sign bit set: signed comparison says this is negative (less than 0-prefixed)
        // GID uses unsigned comparison: f... is greater than 0...
        UUID high = UUID.fromString("f0000000-0000-0000-0000-000000000000");
        UUID low  = UUID.fromString("00000000-0000-0000-0000-000000000001");

        // GID ordering: high > low (unsigned, correct)
        assertTrue(GID.<User>fromUuid(high).compareTo(GID.fromUuid(low)) > 0);

        // Document: UUID.compareTo on JDK <20 would disagree (signed: high < low)
        // On JDK 20+ this divergence is resolved. This test documents the GID contract
        // is always unsigned regardless of JDK version.
    }

    @Test
    void fromString_accepts_charsequence() {
        final GID<User> gid = GID.fromString(new StringBuilder("550e8400-e29b-41d4-a716-446655440000"));
        assertEquals(UUID_A, gid.asUUID());
    }

    @Test
    void fromUUIDs_returns_unmodifiable_list() {
        final var gids = GID.<User>fromUUIDs(java.util.List.of(UUID_A, UUID_B));
        assertThrows(UnsupportedOperationException.class, () -> gids.add(GID.fromUuid(UUID_A)));
    }

    @Test
    void collections_sort_unsigned_ordering() {
        final GID<User> zero = GID.fromString("00000000-0000-0000-0000-000000000000");
        final GID<User> mid  = GID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff");
        final GID<User> high = GID.fromString("80000000-0000-0000-0000-000000000000");
        final GID<User> max  = GID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        List<GID<User>> list = new ArrayList<>(List.of(max, zero, high, mid));
        Collections.sort(list);
        assertEquals(List.of(zero, mid, high, max), list);
    }

    @Test
    void collections_sort_wildcard_list() {
        final GID<User> u = GID.fromString("00000000-0000-0000-0000-000000000002");
        final GID<Organization> o = GID.fromString("00000000-0000-0000-0000-000000000001");

        List<GID<?>> list = new ArrayList<>();
        list.add(u);
        list.add(o);
        Collections.sort(list);
        assertEquals(o, list.get(0));
        assertEquals(u, list.get(1));
    }

    @Test
    void fromUUIDs_null_element() {
        final var uuids = java.util.Arrays.asList(UUID_A, null, UUID_B);
        assertThrows(NullPointerException.class, () -> GID.<User>fromUUIDs(uuids));
    }

    @Test
    void fromUUIDs_round_trip() {
        final var uuids = java.util.List.of(UUID_A, UUID_B);
        final var gids = GID.<User>fromUUIDs(uuids);
        assertEquals(2, gids.size());
        assertEquals(UUID_A, gids.get(0).asUUID());
        assertEquals(UUID_B, gids.get(1).asUUID());
    }
}
