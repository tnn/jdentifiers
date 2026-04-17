package dk.ceti.jdentifiers.id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HexCodecTest {

    @Test
    void getHexValue_valid_digits() {
        assertEquals(0x0, HexCodec.getHexValue('0'));
        assertEquals(0x9, HexCodec.getHexValue('9'));
        assertEquals(0xa, HexCodec.getHexValue('a'));
        assertEquals(0xf, HexCodec.getHexValue('f'));
        assertEquals(0xa, HexCodec.getHexValue('A'));
        assertEquals(0xf, HexCodec.getHexValue('F'));
    }

    @ParameterizedTest
    @CsvSource({
        "'0', 0", "'1', 1", "'2', 2", "'3', 3",
        "'4', 4", "'5', 5", "'6', 6", "'7', 7",
        "'8', 8", "'9', 9",
        "'a', 10", "'b', 11", "'c', 12", "'d', 13", "'e', 14", "'f', 15",
        "'A', 10", "'B', 11", "'C', 12", "'D', 13", "'E', 14", "'F', 15"
    })
    void getHexValue_all_valid_hex_digits(char c, int expected) {
        assertEquals(expected, HexCodec.getHexValue(c));
    }

    static Stream<Character> invalidAsciiChars() {
        Set<Character> valid = Set.of(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f',
            'A', 'B', 'C', 'D', 'E', 'F'
        );
        return IntStream.range(0, 128)
            .mapToObj(i -> (char) i)
            .filter(c -> !valid.contains(c));
    }

    @ParameterizedTest
    @MethodSource("invalidAsciiChars")
    void getHexValue_rejects_all_non_hex_ascii(char c) {
        assertThrows(IllegalArgumentException.class, () -> HexCodec.getHexValue(c));
    }

    @Test
    void getHexValue_non_hex_letter() {
        var ex = assertThrows(IllegalArgumentException.class, () -> HexCodec.getHexValue('g'));
        assertTrue(ex.getMessage().contains("Invalid hexadecimal digit"));
    }

    @Test
    void getHexValue_space() {
        assertThrows(IllegalArgumentException.class, () -> HexCodec.getHexValue(' '));
    }

    @Test
    void getHexValue_del_char_127() {
        // Last valid index into HEX_VALUES array — must still reject
        assertThrows(IllegalArgumentException.class, () -> HexCodec.getHexValue((char) 127));
    }

    @Test
    void getHexValue_char_at_array_boundary() {
        // char 128 — exactly at HEX_VALUES.length, triggers the >= guard
        assertThrows(IllegalArgumentException.class, () -> HexCodec.getHexValue((char) 128));
    }

    @Test
    void getHexValue_max_char_value() {
        // char 65535 — well above HEX_VALUES.length
        assertThrows(IllegalArgumentException.class, () -> HexCodec.getHexValue((char) 65535));
    }

    @Test
    void getHexValue_double_byte_utf8_char() {
        // U+00AB (left-pointing double angle quotation mark) — above array bounds
        assertThrows(IllegalArgumentException.class, () -> HexCodec.getHexValue('\u00AB'));
    }

    @Test
    void getHexValue_null_char() {
        // char 0 is not a hex digit (HEX_VALUES[0] == -1 after Arrays.fill, then '0' (48) is set)
        assertThrows(IllegalArgumentException.class, () -> HexCodec.getHexValue('\0'));
    }

    @Test
    void getHexValue_high_surrogate() {
        assertThrows(IllegalArgumentException.class, () -> HexCodec.getHexValue('\uD800'));
    }
}
