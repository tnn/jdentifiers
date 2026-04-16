package dk.ceti.jdentifiers.id;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * 64-bit identifier stored as an unsigned {@code long}.
 * <p>
 * String representations use big-endian (most-significant-nibble-first) encoding:
 * <ul>
 *   <li>{@link #toString()} / {@link #fromString(CharSequence)} — 16 lowercase hex characters.
 *       Lexicographic hex ordering is consistent with unsigned numeric ordering.</li>
 *   <li>{@link #toBase64String()} / {@link #fromBase64String(CharSequence)} — URL-safe Base64, unpadded.
 *       Base64 string ordering does <em>not</em> match numeric ordering;
 *       use {@link #compareTo} for correct ordering.</li>
 * </ul>
 *
 * @param <T> phantom type for compile-time type safety
 */
public class ID<T extends IDAble> implements Serializable, Comparable<ID<?>> {
    @Serial
    private static final long serialVersionUID = -8420092324658811433L;
    private static final int ID_STRING_LENGTH = 16;
    private final long bits;

    private ID(long bits) {
        this.bits = bits;
    }

    /**
     * Wraps the given long value.
     *
     * @param <R> the entity type
     * @param bits the raw 64-bit value
     * @return a new ID
     */
    public static <R extends IDAble> ID<R> fromLong(long bits) {
        return new ID<>(bits);
    }

    /**
     * Parses a 16-character lowercase hex string into an ID.
     *
     * @param <T> the entity type
     * @param idSequence the hex string (must be exactly 16 characters)
     * @return the parsed ID
     * @throws IllegalArgumentException if the string length is not 16 or contains invalid hex digits
     */
    public static <T extends IDAble> ID<T> fromString(final CharSequence idSequence) {
        Objects.requireNonNull(idSequence, "idSequence must not be null");
        if (idSequence.length() != ID_STRING_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid ID string: expected " + ID_STRING_LENGTH + " hex chars, got " + idSequence.length());
        }

        long bits = (long) HexCodec.getHexValue(idSequence.charAt(0)) << 60;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(1)) << 56;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(2)) << 52;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(3)) << 48;

        bits |= (long) HexCodec.getHexValue(idSequence.charAt(4)) << 44;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(5)) << 40;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(6)) << 36;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(7)) << 32;

        bits |= (long) HexCodec.getHexValue(idSequence.charAt(8)) << 28;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(9)) << 24;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(10)) << 20;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(11)) << 16;

        bits |= (long) HexCodec.getHexValue(idSequence.charAt(12)) << 12;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(13)) << 8;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(14)) << 4;
        bits |= (long) HexCodec.getHexValue(idSequence.charAt(15));

        return new ID<>(bits);
    }

    /**
     * Re-types an ID. Safe because the phantom type is erased at runtime.
     *
     * @param <I> the target entity type
     * @param id the ID to re-type
     * @return the same instance, re-typed
     */
    @SuppressWarnings("unchecked")
    public static <I extends IDAble> ID<I> cast(ID<? extends IDAble> id) {
        return (ID<I>) id;
    }

    /**
     * Returns the underlying {@code long} value.
     *
     * @return the raw bits
     */
    public long asLong() {
        return bits;
    }

    @Override
    public int compareTo(ID<?> o) {
        return Long.compareUnsigned(this.bits, o.bits);
    }

    @Override
    public String toString() {
        final byte[] idChars = new byte[ID_STRING_LENGTH];

        idChars[0] = HexCodec.HEX_DIGITS[(int) ((bits & 0xf000000000000000L) >>> 60)];
        idChars[1] = HexCodec.HEX_DIGITS[(int) ((bits & 0x0f00000000000000L) >>> 56)];
        idChars[2] = HexCodec.HEX_DIGITS[(int) ((bits & 0x00f0000000000000L) >>> 52)];
        idChars[3] = HexCodec.HEX_DIGITS[(int) ((bits & 0x000f000000000000L) >>> 48)];
        idChars[4] = HexCodec.HEX_DIGITS[(int) ((bits & 0x0000f00000000000L) >>> 44)];
        idChars[5] = HexCodec.HEX_DIGITS[(int) ((bits & 0x00000f0000000000L) >>> 40)];
        idChars[6] = HexCodec.HEX_DIGITS[(int) ((bits & 0x000000f000000000L) >>> 36)];
        idChars[7] = HexCodec.HEX_DIGITS[(int) ((bits & 0x0000000f00000000L) >>> 32)];
        idChars[8] = HexCodec.HEX_DIGITS[(int) ((bits & 0x00000000f0000000L) >>> 28)];
        idChars[9] = HexCodec.HEX_DIGITS[(int) ((bits & 0x000000000f000000L) >>> 24)];
        idChars[10] = HexCodec.HEX_DIGITS[(int) ((bits & 0x0000000000f00000L) >>> 20)];
        idChars[11] = HexCodec.HEX_DIGITS[(int) ((bits & 0x00000000000f0000L) >>> 16)];
        idChars[12] = HexCodec.HEX_DIGITS[(int) ((bits & 0x000000000000f000L) >>> 12)];
        idChars[13] = HexCodec.HEX_DIGITS[(int) ((bits & 0x0000000000000f00L) >>> 8)];
        idChars[14] = HexCodec.HEX_DIGITS[(int) ((bits & 0x00000000000000f0L) >>> 4)];
        idChars[15] = HexCodec.HEX_DIGITS[(int) (bits & 0x000000000000000fL)];

        return new String(idChars, StandardCharsets.ISO_8859_1);
    }

    /**
     * Returns a URL-safe, unpadded Base64 encoding of this ID in big-endian byte order.
     *
     * @return the Base64 string
     * @see #fromBase64String(CharSequence)
     */
    public String toBase64String() {
        byte[] b = new byte[Long.BYTES];
        b[0] = (byte) (bits >> 56);
        b[1] = (byte) (bits >> 48);
        b[2] = (byte) (bits >> 40);
        b[3] = (byte) (bits >> 32);
        b[4] = (byte) (bits >> 24);
        b[5] = (byte) (bits >> 16);
        b[6] = (byte) (bits >> 8);
        b[7] = (byte) bits;
        return new String(Base64.getUrlEncoder().withoutPadding().encode(b), StandardCharsets.ISO_8859_1);
    }

    /**
     * Creates an ID from a URL-safe Base64 string (padded or unpadded) in big-endian byte order.
     * <p>
     * Accepts {@link CharSequence} for API consistency; internally calls {@code toString()}.
     *
     * @param <T> the entity type
     * @param base64 the Base64 encoded string
     * @return the decoded ID
     * @throws IllegalArgumentException if the decoded bytes are not exactly 8
     * @see #toBase64String()
     */
    public static <T extends IDAble> ID<T> fromBase64String(CharSequence base64) {
        Objects.requireNonNull(base64, "base64 must not be null");
        byte[] b = Base64.getUrlDecoder().decode(base64.toString());
        if (b.length != Long.BYTES) {
            throw new IllegalArgumentException(
                "Invalid base64 ID: expected 8 bytes, got " + b.length);
        }
        long bits = ((long) (b[0] & 0xFF) << 56)
                  | ((long) (b[1] & 0xFF) << 48)
                  | ((long) (b[2] & 0xFF) << 40)
                  | ((long) (b[3] & 0xFF) << 32)
                  | ((long) (b[4] & 0xFF) << 24)
                  | ((long) (b[5] & 0xFF) << 16)
                  | ((long) (b[6] & 0xFF) << 8)
                  |  (long) (b[7] & 0xFF);
        return new ID<>(bits);
    }

    /**
     * Compares based on the underlying {@code long} value only.
     * The phantom type parameter {@code T} is erased at runtime and is not considered.
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ID<? extends IDAble> id = (ID<? extends IDAble>) o;
        return bits == id.bits;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(bits);
    }
}
