package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
    void toHexString_returns_the_dashless_lowercase_hex() {
        final GID<User> gid = GID.fromUuid(UUID_A);
        assertEquals("550e8400e29b41d4a716446655440000", gid.toHexString());
    }

    @Test
    void toHexString_keeps_leading_zeros_and_the_high_bit() {
        assertEquals("00000000000000010000000000000002", GID.fromUuid(new UUID(1L, 2L)).toHexString());
        assertEquals("ffffffffffffffff8000000000000000", GID.fromUuid(new UUID(-1L, Long.MIN_VALUE)).toHexString());
    }

    @Test
    void toHexString_round_trips_through_parseLenient() {
        final GID<User> gid = GID.<User>parseLenient("0af7651916cd43dd8448eb211c80319c").orElseThrow();
        assertEquals("0af7651916cd43dd8448eb211c80319c", gid.toHexString());
        assertEquals(gid, GID.parseLenient(gid.toHexString()).orElseThrow());
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
        UUID low = UUID.fromString("00000000-0000-0000-0000-000000000001");

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
        final var gids = GID.<User>fromUUIDs(List.of(UUID_A, UUID_B));
        assertThrows(UnsupportedOperationException.class, () -> gids.add(GID.fromUuid(UUID_A)));
    }

    @Test
    void collections_sort_unsigned_ordering() {
        final GID<User> zero = GID.fromString("00000000-0000-0000-0000-000000000000");
        final GID<User> mid = GID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff");
        final GID<User> high = GID.fromString("80000000-0000-0000-0000-000000000000");
        final GID<User> max = GID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

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
        final var uuids = Arrays.asList(UUID_A, null, UUID_B);
        assertThrows(NullPointerException.class, () -> GID.<User>fromUUIDs(uuids));
    }

    @Test
    void fromUUIDs_round_trip() {
        final var uuids = List.of(UUID_A, UUID_B);
        final var gids = GID.<User>fromUUIDs(uuids);
        assertEquals(2, gids.size());
        assertEquals(UUID_A, gids.get(0).asUUID());
        assertEquals(UUID_B, gids.get(1).asUUID());
    }


    // ------------------------------------------------------------------
    // GID.fromString mirrors java.util.UUID.fromString (lenient, no surprises).
    // Strict canonical validation lives in parseStrict (below).
    // ------------------------------------------------------------------

    @Test
    void fromString_accepts_uppercase_canonical() {
        final GID<User> gid = GID.fromString("550E8400-E29B-41D4-A716-446655440000");
        assertEquals(UUID_A, gid.asUUID());
    }

    @Test
    void fromString_accepts_mixed_case_canonical() {
        assertEquals(UUID_A, GID.<User>fromString("550e8400-E29B-41d4-A716-446655440000").asUUID());
    }

    @Test
    void fromString_lenient_like_uuid_short_groups() {
        // UUID.fromString accepts short groups; GID.fromString mirrors it.
        assertEquals(
            UUID.fromString("1-1-1-1-1"),
            GID.<User>fromString("1-1-1-1-1").asUUID());
    }

    @Test
    void fromString_lenient_like_uuid_sign_prefix() {
        // The "sign hole": UUID.fromString coerces a leading '+' into a sign.
        // GID.fromString matches that (use parseStrict to reject it).
        assertEquals(
            UUID.fromString("+e83dd89-d106-406c-8eff-53864a4b2d13"),
            GID.<User>fromString("+e83dd89-d106-406c-8eff-53864a4b2d13").asUUID());
    }

    @Test
    void uuid_fromString_sign_hole_coerces_to_zero_prefix() {
        // Verifies the coercion claim in GID.fromString's javadoc against the
        // running JDK: "+e83dd89" parses to the same number as "0e83dd89"
        // (Long.parseLong treats '+' as a sign, then 7 hex digits — value
        // 0xe83dd89, which equals 0x0e83dd89). If a future JDK closes this
        // hole UUID.fromString will throw and this test will fail loudly.
        assertEquals(
            UUID.fromString("0e83dd89-d106-406c-8eff-53864a4b2d13"),
            UUID.fromString("+e83dd89-d106-406c-8eff-53864a4b2d13"));
    }

    @Test
    void fromString_rejects_what_uuid_rejects() {
        // Reject-side parity with UUID.fromString: extra dash, non-hex, over-long.
        assertThrows(IllegalArgumentException.class,
            () -> GID.fromString("-e83dd89-d106-406c-8eff-53864a4b2d13"));
        assertThrows(IllegalArgumentException.class,
            () -> GID.fromString("gggggggg-gggg-gggg-gggg-gggggggggggg"));
        assertThrows(IllegalArgumentException.class,
            () -> GID.fromString("550e8400-e29b-41d4-a716-4466554400000"));
    }

    // ------------------------------------------------------------------
    // GID.parseStrict accepts ONLY the canonical 8-4-4-4-12 hex form
    // (case-insensitive) and returns empty otherwise; never throws. It rejects
    // the non-canonical forms UUID.fromString / GID.fromString would accept.
    // ------------------------------------------------------------------

    @Test
    void parseStrict_valid_uuid() {
        var result = GID.<User>parseStrict("01234567-89ab-7def-8123-456789abcdef");
        assertTrue(result.isPresent());
        assertEquals("01234567-89ab-7def-8123-456789abcdef", result.get().toString());
    }

    @Test
    void parseStrict_accepts_upper_mixed_nil_max() {
        assertTrue(GID.parseStrict("550E8400-E29B-41D4-A716-446655440000").isPresent());
        assertTrue(GID.parseStrict("550e8400-E29B-41d4-A716-446655440000").isPresent());
        assertTrue(GID.parseStrict("00000000-0000-0000-0000-000000000000").isPresent());
        assertTrue(GID.parseStrict("ffffffff-ffff-ffff-ffff-ffffffffffff").isPresent());
    }

    @Test
    void parseStrict_null() {
        assertTrue(GID.parseStrict(null).isEmpty());
    }

    @Test
    void parseStrict_empty() {
        assertTrue(GID.parseStrict("").isEmpty());
    }

    @Test
    void parseStrict_rejects_not_a_uuid() {
        assertTrue(GID.parseStrict("not-a-uuid").isEmpty());
    }

    @Test
    void parseStrict_rejects_short_segments() {
        assertTrue(GID.parseStrict("1-1-1-1-1").isEmpty());
    }

    @Test
    void parseStrict_rejects_leading_plus_sign() {
        assertTrue(GID.parseStrict("+e83dd89-d106-406c-8eff-53864a4b2d13").isEmpty());
    }

    @Test
    void parseStrict_rejects_plus_sign_in_later_group() {
        assertTrue(GID.parseStrict("ee83dd89-+106-406c-8eff-53864a4b2d13").isEmpty());
    }

    @Test
    void parseStrict_rejects_leading_minus_sign() {
        assertTrue(GID.parseStrict("-e83dd89-d106-406c-8eff-53864a4b2d13").isEmpty());
    }

    @Test
    void parseStrict_rejects_oversized_segment() {
        assertTrue(GID.parseStrict("00000000-0000-0000-0000-0000000000001").isEmpty());
    }

    @Test
    void parseStrict_rejects_misplaced_dash() {
        assertTrue(GID.parseStrict("de83dd8-9d106-406c-8eff-53864a4b2d13").isEmpty());
    }

    @Test
    void parseStrict_rejects_non_hex_char() {
        assertTrue(GID.parseStrict("gggggggg-gggg-gggg-gggg-gggggggggggg").isEmpty());
    }

    @Test
    void parseStrict_rejects_trailing_whitespace() {
        assertTrue(GID.parseStrict("550e8400-e29b-41d4-a716-446655440000 ").isEmpty());
    }

    @Test
    void parseStrict_rejects_too_short() {
        assertTrue(GID.parseStrict("550e8400-e29b-41d4-a716-44665544000").isEmpty());
    }

    @Test
    void parseStrict_rejects_too_long() {
        assertTrue(GID.parseStrict("550e8400-e29b-41d4-a716-4466554400000").isEmpty());
    }

    // ------------------------------------------------------------------
    // GID.parseLenient accepts BOTH the dashed UUID forms and the dashless
    // 32-char hex form (e.g. a W3C traceparent trace-id). Never throws.
    // ------------------------------------------------------------------

    @Test
    void parseLenient_accepts_canonical_dashed() {
        var result = GID.<User>parseLenient("4bf92f35-77b3-4da6-a3ce-929d0e0e4736");
        assertTrue(result.isPresent());
        assertEquals("4bf92f35-77b3-4da6-a3ce-929d0e0e4736", result.get().toString());
    }

    @Test
    void parseLenient_accepts_dashless_32_hex() {
        // W3C traceparent trace-id form: 32 hex chars, no dashes.
        var result = GID.<User>parseLenient("4bf92f3577b34da6a3ce929d0e0e4736");
        assertTrue(result.isPresent());
        assertEquals("4bf92f35-77b3-4da6-a3ce-929d0e0e4736", result.get().toString());
    }

    @Test
    void parseLenient_dashless_matches_dashed() {
        assertEquals(
            GID.<User>parseLenient("4bf92f35-77b3-4da6-a3ce-929d0e0e4736"),
            GID.<User>parseLenient("4bf92f3577b34da6a3ce929d0e0e4736"));
    }

    @Test
    void parseLenient_accepts_lenient_dashed_form() {
        // Delegates to fromString for dashed input, so UUID.fromString leniency applies.
        assertTrue(GID.parseLenient("1-1-1-1-1").isPresent());
    }

    @Test
    void parseLenient_null() {
        assertTrue(GID.parseLenient(null).isEmpty());
    }

    @Test
    void parseLenient_rejects_31_hex() {
        assertTrue(GID.parseLenient("4bf92f3577b34da6a3ce929d0e0e473").isEmpty());
    }

    @Test
    void parseLenient_rejects_non_hex_32() {
        assertTrue(GID.parseLenient("gggggggggggggggggggggggggggggggg").isEmpty());
    }
}
