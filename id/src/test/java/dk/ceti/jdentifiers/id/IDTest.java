package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IDTest {
    private static IDGenerator generator;

    @BeforeAll
    static void setupSpec() {
        generator = new RandomIDGenerator();
    }

    @Test
    void should_get_user_id_from_string() {
        final ID<A> id = ID.fromString("6a677fc2ee05e1f6");
        assertNotNull(id);
        assertEquals(7667237365815304694L, id.asLong());
    }

    @Test
    void should_not_accept_null_as_string() {
        var ex = assertThrows(NullPointerException.class, () -> ID.fromString(null));
        assertEquals("idSequence must not be null", ex.getMessage());
    }

    @Test
    void should_not_accept_to_many_components() {
        assertThrows(IllegalArgumentException.class, () -> ID.fromString("8a675-6fc2ee-5e1f-6"));
    }

    @Test
    void should_not_accept_to_few_components() {
        assertThrows(IllegalArgumentException.class, () -> ID.fromString("8a675-6fc2ee5e1f6"));
    }

    @Test
    void should_be_able_to_get_user_id_from_long() {
        final ID<A> id = ID.fromLong(-8473663698680552970L);
        assertNotNull(id);
        assertEquals(-8473663698680552970L, id.asLong());
    }

    @Test
    void should_be_able_to_id_string_representation() {
        final String id = ID.fromLong(-8473663698680552970L).toString();
        assertNotNull(id);
        assertEquals("8a677fc2ee05e1f6", id);
    }

    @Test
    void lower_than_min_long_should_overflow_around() {
        assertEquals(-9223372036854775807L, ID.fromString("8000000000000001").asLong());
    }

    @Test
    void should_be_able_to_generate_random_id() {
        assertNotNull(generator.identifier());
    }

    @Test
    void upper_case_should_be_equal() {
        assertEquals(ID.fromString("6a677fc2ee05e1f6"), ID.fromString("6A677FC2EE05E1F6"));
    }

    @Test
    void non_hex_char_should_throw_exception() {
        assertThrows(IllegalArgumentException.class, () -> ID.fromString("xa677fc2ee05e1f6"));
    }

    @Test
    void double_byte_utf8_char_should_throw_exception() {
        assertThrows(IllegalArgumentException.class, () -> ID.fromString("\u00ABa677fc2ee05e1f6"));
    }

    @Test
    void upper_case_should_have_same_hash_code() {
        assertEquals(ID.fromString("6a677fc2ee05e1f6").hashCode(),
            ID.fromString("6A677FC2EE05E1F6").hashCode()
        );
    }

    @Test
    void same_instance_should_be_equal() {
        final ID<A> id = ID.fromString("6A677FC2EE05E1F6");
        assertEquals(id, id);
    }

    @Test
    void null_should_not_be_equal() {
        assertFalse(ID.fromString("6A677FC2EE05E1F6").equals(null));
    }

    @Test
    void different_class_should_not_be_equal() {
        assertFalse(ID.fromString("6A677FC2EE05E1F6").equals(UUID.randomUUID()));
    }

    @Test
    void compareTo_less_than() {
        assertTrue(ID.<A>fromString("6a677fc2ee05e1f6").compareTo(ID.fromString("8a677fc2ee05e1f6")) < 0);
    }

    @Test
    void compareTo_equal() {
        assertEquals(0, ID.<A>fromString("6a677fc2ee05e1f6").compareTo(ID.fromString("6a677fc2ee05e1f6")));
    }

    @Test
    void compareTo_unsigned_ordering() {
        // 8... has high bit set — signed comparison would make this negative, sorting before 6...
        assertTrue(ID.<A>fromString("8a677fc2ee05e1f6").compareTo(ID.fromString("6a677fc2ee05e1f6")) > 0);
    }

    @Test
    void collections_sort_unsigned_ordering() {
        final ID<A> zero = ID.fromLong(0L);
        final ID<A> mid = ID.fromLong(Long.MAX_VALUE);
        final ID<A> high = ID.fromLong(Long.MIN_VALUE);
        final ID<A> max = ID.fromLong(-1L);

        List<ID<A>> list = new ArrayList<>(List.of(max, zero, high, mid));
        Collections.sort(list);
        assertEquals(List.of(zero, mid, high, max), list);
    }

    @Test
    void collections_sort_wildcard_list() {
        final ID<A> a = ID.fromLong(2L);
        final ID<B> b = ID.fromLong(1L);

        List<ID<?>> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        Collections.sort(list);
        assertEquals(1L, list.get(0).asLong());
    }

    @Test
    void java_serialization_round_trip() throws Exception {
        final ID<A> original = ID.fromLong(7667237365815304694L);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(original);
        ID<?> deserialized = (ID<?>) new ObjectInputStream(
            new ByteArrayInputStream(baos.toByteArray())).readObject();
        assertEquals(original, deserialized);
    }

    @Test
    void fromBase64String_empty_string() {
        assertThrows(IllegalArgumentException.class, () -> ID.<A>fromBase64String(""));
    }

    @Test
    void base64_round_trip() {
        final ID<A> id = ID.fromLong(7667237365815304694L);
        assertEquals(id, ID.<A>fromBase64String(id.toBase64String()));
    }

    @Test
    void base64_round_trip_high_bit() {
        final ID<A> id = ID.fromLong(-1L);
        assertEquals(id, ID.<A>fromBase64String(id.toBase64String()));
    }

    @Test
    void base64_round_trip_zero() {
        final ID<A> id = ID.fromLong(0L);
        assertEquals(id, ID.<A>fromBase64String(id.toBase64String()));
    }

    @Test
    void toBase64String_unpadded() {
        String base64 = ID.fromLong(7667237365815304694L).toBase64String();
        assertFalse(base64.contains("="));
        assertEquals(11, base64.length());
    }

    @Test
    void fromBase64String_invalid_length() {
        assertThrows(IllegalArgumentException.class, () -> ID.<A>fromBase64String("AAAA"));
    }

    @Test
    void fromBase64String_null() {
        var ex = assertThrows(NullPointerException.class, () -> ID.<A>fromBase64String(null));
        assertEquals("base64 must not be null", ex.getMessage());
    }

    @Test
    void can_cast_between_ids() {
        final ID<A> a = generator.identifier();
        final ID<B> b = ID.fromLong(a.asLong());
        assertEquals(a, ID.cast(b));
    }

    // --- Boundary value tests with pinned string assertions ---

    static Stream<Arguments> boundaryValues() {
        return Stream.of(
            Arguments.of(0L, "0000000000000000"),
            Arguments.of(1L, "0000000000000001"),
            Arguments.of(-1L, "ffffffffffffffff"),
            Arguments.of(Long.MAX_VALUE, "7fffffffffffffff"),
            Arguments.of(Long.MIN_VALUE, "8000000000000000"),
            Arguments.of(0x0123456789abcdefL, "0123456789abcdef"),
            Arguments.of(0xfedcba9876543210L, "fedcba9876543210")
        );
    }

    @ParameterizedTest
    @MethodSource("boundaryValues")
    void toString_pinned_boundary_values(long value, String expectedHex) {
        assertEquals(expectedHex, ID.fromLong(value).toString());
    }

    @ParameterizedTest
    @MethodSource("boundaryValues")
    void fromString_pinned_boundary_values(long expectedValue, String hex) {
        assertEquals(expectedValue, ID.<A>fromString(hex).asLong());
    }

    @ParameterizedTest
    @MethodSource("boundaryValues")
    void toString_always_16_chars(long value, String expectedHex) {
        assertEquals(16, ID.fromLong(value).toString().length());
    }

    @Test
    void toString_output_is_always_lowercase() {
        String hex = ID.fromLong(0xABCDEF0123456789L).toString();
        assertEquals(hex, hex.toLowerCase());
    }

    @Test
    void fromString_empty_string() {
        assertThrows(IllegalArgumentException.class, () -> ID.<A>fromString(""));
    }

    static Stream<Integer> allPositions16() {
        return IntStream.range(0, 16).boxed();
    }

    @ParameterizedTest
    @MethodSource("allPositions16")
    void fromString_invalid_char_at_every_position(int position) {
        char[] chars = "0123456789abcdef".toCharArray();
        chars[position] = 'g';
        assertThrows(IllegalArgumentException.class, () -> ID.<A>fromString(new String(chars)));
    }

    @Test
    void fromString_accepts_StringBuilder() {
        StringBuilder sb = new StringBuilder("6a677fc2ee05e1f6");
        ID<A> id = ID.fromString(sb);
        assertEquals(7667237365815304694L, id.asLong());
    }

    private static final class A implements IDAble {
    }

    private static final class B implements IDAble {
    }
}
