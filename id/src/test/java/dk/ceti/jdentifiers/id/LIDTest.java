package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
