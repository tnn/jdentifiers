package org.pkgd.jdentifiers.id;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IDTest {
    private static IDGenerator generator;

    @BeforeAll
    static void setupSpec() {
        generator = new RandomIDGenerator();
    }

    @Test
    void should_get_user_id_from_string() {
        // binary bbinaryy               = 8  (100.0%) -63.6%
        // base64 amd/wu4F4fY            = 11 (137.5%) -50.0%
        // base32 NJTX7QXOAXQ7M          = 13 (162.5%) -40.9%
        // base16 6a677fc2ee05e1f6       = 16 (200.0%) -27.3%  <--
        // base10 1061031271942385225246 = 22 (275.0%)

        final ID<A> id = ID.fromString("6a677fc2ee05e1f6");
        assertNotNull(id);
        assertEquals(7667237365815304694L, id.asLong());
    }

    @Test
    void should_not_accept_null_as_string() {
        assertThrows(NullPointerException.class, () -> ID.fromString(null));
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
                ID.fromString("6A677FC2EE05E1F6").hashCode());
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
    void can_cast_between_ids() {
        final ID<A> a = generator.identifier();
        final ID<B> b = ID.fromLong(a.asLong());
        assertEquals(a, ID.cast(b));
    }

    private static final class A implements IDAble {
    }

    private static final class B implements IDAble {
    }
}
