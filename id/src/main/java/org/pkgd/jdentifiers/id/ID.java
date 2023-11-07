package org.pkgd.jdentifiers.id;

import org.pkgd.jdentifiers.id.base32.Base32Crockford;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * 64-bit identifier.
 * <p>
 *
 * @param <T>
 */
public class ID<T extends IDAble> extends VariableLengthID implements Serializable, Comparator<ID<T>> {
    private static final int ID_STRING_LENGTH = 16;

    private final long bits;

    private ID(long bits) {
        this.bits = bits;
    }

    /**
     * Create instance from base32 string.
     */
    public static <R extends IDAble> ID<R> fromString(String str) {
        try {
            return ID.fromLong(Base32Crockford.decode(str));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create instance from primitive long.
     */
    public static <R extends IDAble> ID<R> fromLong(long bits) {
        return new ID<>(bits);
    }

    public static <T extends IDAble> ID<T> fromHexString(final CharSequence idSequence) {
        if (idSequence.length() != ID_STRING_LENGTH) {
            throw new IllegalArgumentException("Illegal ID64 string: " + idSequence);
        }

        long bits = getHexValueForChar(idSequence.charAt(0)) << 60;
        bits |= getHexValueForChar(idSequence.charAt(1)) << 56;
        bits |= getHexValueForChar(idSequence.charAt(2)) << 52;
        bits |= getHexValueForChar(idSequence.charAt(3)) << 48;

        bits |= getHexValueForChar(idSequence.charAt(4)) << 44;
        bits |= getHexValueForChar(idSequence.charAt(5)) << 40;
        bits |= getHexValueForChar(idSequence.charAt(6)) << 36;
        bits |= getHexValueForChar(idSequence.charAt(7)) << 32;

        bits |= getHexValueForChar(idSequence.charAt(8)) << 28;
        bits |= getHexValueForChar(idSequence.charAt(9)) << 24;
        bits |= getHexValueForChar(idSequence.charAt(10)) << 20;
        bits |= getHexValueForChar(idSequence.charAt(11)) << 16;

        bits |= getHexValueForChar(idSequence.charAt(12)) << 12;
        bits |= getHexValueForChar(idSequence.charAt(13)) << 8;
        bits |= getHexValueForChar(idSequence.charAt(14)) << 4;
        bits |= getHexValueForChar(idSequence.charAt(15));

        return new ID<>(bits);
    }

    @SuppressWarnings("unchecked")
    public static <I extends IDAble> ID<I> cast(ID<? extends IDAble> id) {
        return (ID<I>) id;
    }

    private static long getHexValueForChar(final char c) {
        try {
            if (HEX_VALUES[c] < 0) {
                throw new IllegalArgumentException("Illegal hexadecimal digit: " + c);
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Illegal hexadecimal digit: " + c, e);
        }

        return HEX_VALUES[c];
    }

    public long asLong() {
        return bits;
    }

    @Override
    public int compare(ID<T> x, ID<T> y) {
        return Long.compare(x.bits, y.bits);
    }

    @Override
    public String toString() {
        return Base32Crockford.encode(bits);
    }

    @Override
    public String toHexString() {
        final char[] idChars = new char[ID_STRING_LENGTH];

        idChars[0] = HEX_DIGITS[(int) ((bits & 0xf000000000000000L) >>> 60)];
        idChars[1] = HEX_DIGITS[(int) ((bits & 0x0f00000000000000L) >>> 56)];
        idChars[2] = HEX_DIGITS[(int) ((bits & 0x00f0000000000000L) >>> 52)];
        idChars[3] = HEX_DIGITS[(int) ((bits & 0x000f000000000000L) >>> 48)];
        idChars[4] = HEX_DIGITS[(int) ((bits & 0x0000f00000000000L) >>> 44)];
        idChars[5] = HEX_DIGITS[(int) ((bits & 0x00000f0000000000L) >>> 40)];
        idChars[6] = HEX_DIGITS[(int) ((bits & 0x000000f000000000L) >>> 36)];
        idChars[7] = HEX_DIGITS[(int) ((bits & 0x0000000f00000000L) >>> 32)];
        idChars[8] = HEX_DIGITS[(int) ((bits & 0x00000000f0000000L) >>> 28)];
        idChars[9] = HEX_DIGITS[(int) ((bits & 0x000000000f000000L) >>> 24)];
        idChars[10] = HEX_DIGITS[(int) ((bits & 0x0000000000f00000L) >>> 20)];
        idChars[11] = HEX_DIGITS[(int) ((bits & 0x00000000000f0000L) >>> 16)];
        idChars[12] = HEX_DIGITS[(int) ((bits & 0x000000000000f000L) >>> 12)];
        idChars[13] = HEX_DIGITS[(int) ((bits & 0x0000000000000f00L) >>> 8)];
        idChars[14] = HEX_DIGITS[(int) ((bits & 0x00000000000000f0L) >>> 4)];
        idChars[15] = HEX_DIGITS[(int) (bits & 0x000000000000000fL)];

        return new String(idChars);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ID<? extends IDAble> id64 = (ID<? extends IDAble>) o;
        return bits == id64.bits;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bits);
    }
}
