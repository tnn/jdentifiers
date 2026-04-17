package dk.ceti.jdentifiers.id;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Locally scoped 32-bit identifier stored as an unsigned {@code int}.
 * <p>
 * The string representation ({@link #toString()} / {@link #fromString(CharSequence)})
 * uses 8 lowercase hex characters in big-endian (most-significant-nibble-first) encoding.
 * This ensures that lexicographic string ordering is consistent with unsigned numeric ordering.
 *
 * @param <T> phantom type for compile-time type safety
 */
public class LID<T extends IDAble> implements Serializable, Comparable<LID<?>> {
    @Serial
    private static final long serialVersionUID = -2800087606778454906L;
    private static final int LID_STRING_LENGTH = 8;
    private final int bits;

    private LID(int bits) {
        this.bits = bits;
    }

    /**
     * Wraps the given int value.
     *
     * @param <T> the entity type
     * @param bits the raw 32-bit value
     * @return a new LID
     */
    public static <T extends IDAble> LID<T> fromInteger(int bits) {
        return new LID<>(bits);
    }

    /**
     * Parses an 8-character lowercase hex string into a LID.
     *
     * @param <T> the entity type
     * @param idSequence the hex string (must be exactly 8 characters)
     * @return the parsed LID
     * @throws IllegalArgumentException if the string length is not 8 or contains invalid hex digits
     */
    public static <T extends IDAble> LID<T> fromString(final CharSequence idSequence) {
        Objects.requireNonNull(idSequence, "idSequence must not be null");
        if (idSequence.length() != LID_STRING_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid ID string: expected " + LID_STRING_LENGTH + " hex chars, got " + idSequence.length());
        }

        int bits = HexCodec.getHexValue(idSequence.charAt(0)) << 28;
        bits |= HexCodec.getHexValue(idSequence.charAt(1)) << 24;
        bits |= HexCodec.getHexValue(idSequence.charAt(2)) << 20;
        bits |= HexCodec.getHexValue(idSequence.charAt(3)) << 16;

        bits |= HexCodec.getHexValue(idSequence.charAt(4)) << 12;
        bits |= HexCodec.getHexValue(idSequence.charAt(5)) << 8;
        bits |= HexCodec.getHexValue(idSequence.charAt(6)) << 4;
        bits |= HexCodec.getHexValue(idSequence.charAt(7));

        return new LID<>(bits);
    }

    /**
     * Re-types a LID. Safe because the phantom type is erased at runtime.
     *
     * @param <I> the target entity type
     * @param id the LID to re-type
     * @return the same instance, re-typed
     */
    @SuppressWarnings("unchecked")
    public static <I extends IDAble> LID<I> cast(LID<? extends IDAble> id) {
        return (LID<I>) id;
    }

    /**
     * Returns the underlying {@code int} value.
     *
     * @return the raw bits
     */
    public int toInteger() {
        return bits;
    }

    @Override
    public int compareTo(LID<?> o) {
        return Integer.compareUnsigned(this.bits, o.bits);
    }

    /**
     * Compares based on the underlying {@code int} value only.
     * The phantom type parameter {@code T} is erased at runtime and is not considered.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final LID<? extends IDAble> id32 = (LID<? extends IDAble>) o;
        return bits == id32.bits;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(bits);
    }

    @Override
    public String toString() {
        final byte[] idChars = new byte[LID_STRING_LENGTH];

        idChars[0] = HexCodec.HEX_DIGITS[(bits & 0xf0000000) >>> 28];
        idChars[1] = HexCodec.HEX_DIGITS[(bits & 0x0f000000) >>> 24];
        idChars[2] = HexCodec.HEX_DIGITS[(bits & 0x00f00000) >>> 20];
        idChars[3] = HexCodec.HEX_DIGITS[(bits & 0x000f0000) >>> 16];
        idChars[4] = HexCodec.HEX_DIGITS[(bits & 0x0000f000) >>> 12];
        idChars[5] = HexCodec.HEX_DIGITS[(bits & 0x00000f00) >>> 8];
        idChars[6] = HexCodec.HEX_DIGITS[(bits & 0x000000f0) >>> 4];
        idChars[7] = HexCodec.HEX_DIGITS[bits & 0x0000000f];

        return new String(idChars, StandardCharsets.ISO_8859_1);
    }
}
