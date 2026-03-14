package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.BeforeAll;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LIDTest {
    private static IDGenerator generator;

    @BeforeAll
    static void setupSpec() {
        generator = new RandomIDGenerator();
    }

    @Test
    void should_get_user_id_from_string() {
        final LID<A> id = LID.fromString("6a677fc2");
        assertNotNull(id);
        assertEquals(1785167810, id.toInteger());
    }

    @Test
    void should_not_accept_null_as_string() {
        assertThrows(NullPointerException.class, () -> LID.fromString(null));
    }

    @Test
    void should_not_accept_to_many_components() {
        assertThrows(IllegalArgumentException.class, () -> LID.<A>fromString("8a675-6fc2ee-5e1f-6"));
    }

    @Test
    void should_not_accept_to_few_components() {
        assertThrows(IllegalArgumentException.class, () -> LID.<A>fromString("8a675-6fc2ee5e1f6"));
    }

    @Test
    void should_be_able_to_get_user_id_from_long() {
        final LID<A> id = LID.fromInteger(-847366369);
        assertNotNull(id);
        assertEquals(-847366369, id.toInteger());
    }

    @Test
    void should_be_able_to_id_hex_string_representation() {
        final String id = LID.fromInteger(-847366369).toString();
        assertNotNull(id);
        assertEquals("cd7e371f", id);
    }

    @Test
    void lower_than_min_integer_should_wrap_around() {
        assertEquals(-2147483647, LID.fromString("80000001").toInteger());
    }

    @Test
    void should_be_able_to_generate_random_id() {
        assertNotNull(generator.localIdentifier());
    }

    @Test
    void non_hex_char_should_throw_exception() {
        assertThrows(IllegalArgumentException.class, () -> LID.<A>fromString("xa677fc2"));
    }

    @Test
    void double_byte_utf8_char_should_throw_exception() {
        assertThrows(IllegalArgumentException.class, () -> LID.<A>fromString("\u00ABa677fc2"));
    }

    @Test
    void upper_case_should_be_equal() {
        assertEquals(LID.<A>fromString("6a677fc2"), LID.<A>fromString("6A677FC2"));
    }

    @Test
    void upper_case_should_have_same_hash_code() {
        assertEquals(LID.<A>fromString("6a677fc2").hashCode(), LID.<A>fromString("6A677FC2").hashCode());
    }

    @Test
    void same_instance_should_be_equal() {
        final LID<B> id = LID.fromString("6a677fc2");
        assertEquals(id, id);
    }

    @Test
    void null_should_not_be_equal() {
        assertFalse(LID.fromString("6a677fc2").equals(null));
    }

    @Test
    void different_class_should_not_be_equal() {
        assertFalse(LID.fromString("6a677fc2").equals(UUID.randomUUID()));
    }

    @Test
    void compareTo_less_than() {
        assertTrue(LID.<A>fromString("6a677fc2").compareTo(LID.fromString("8a677fc2")) < 0);
    }

    @Test
    void compareTo_equal() {
        assertEquals(0, LID.<A>fromString("6a677fc2").compareTo(LID.fromString("6a677fc2")));
    }

    @Test
    void compareTo_unsigned_ordering() {
        assertTrue(LID.<A>fromString("8a677fc2").compareTo(LID.fromString("6a677fc2")) > 0);
    }

    @Test
    void java_serialization_round_trip() throws Exception {
        final LID<A> original = LID.fromInteger(1785167810);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(original);
        LID<?> deserialized = (LID<?>) new ObjectInputStream(
            new ByteArrayInputStream(baos.toByteArray())).readObject();
        assertEquals(original, deserialized);
    }

    @Test
    void collections_sort_unsigned_ordering() {
        final LID<A> zero = LID.fromInteger(0);
        final LID<A> mid  = LID.fromInteger(Integer.MAX_VALUE);
        final LID<A> high = LID.fromInteger(Integer.MIN_VALUE);
        final LID<A> max  = LID.fromInteger(-1);

        List<LID<A>> list = new ArrayList<>(List.of(max, zero, high, mid));
        Collections.sort(list);
        assertEquals(List.of(zero, mid, high, max), list);
    }

    @Test
    void collections_sort_wildcard_list() {
        final LID<A> a = LID.fromInteger(2);
        final LID<B> b = LID.fromInteger(1);

        List<LID<?>> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        Collections.sort(list);
        assertEquals(1, list.get(0).toInteger());
    }

    @Test
    void can_cast_between_ids() {
        LID<A> a = generator.localIdentifier();
        LID<B> b = LID.fromInteger(a.toInteger());
        assertEquals(a, LID.cast(b));
    }

    private static final class A implements IDAble {
    }

    private static final class B implements IDAble {
    }
}
