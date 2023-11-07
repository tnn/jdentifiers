package org.pkgd.jdentifiers.id;

import java.util.Objects;

/**
 * Locally scoped 32-bit identifier.
 */
public class LID<T extends IDAble> extends VariableLengthID {
    private static final int LID_STRING_LENGTH = 8;

    private final int bits;

    private LID(int bits) {
        this.bits = bits;
    }

    public static <T extends IDAble> LID<T> fromInteger(int bits) {
        return new LID<>(bits);
    }

    public static <T extends IDAble> LID<T> fromHexString(final CharSequence idSequence) {
        if (idSequence.length() != LID_STRING_LENGTH) {
            throw new IllegalArgumentException("Illegal LID hex string: " + idSequence);
        }

        int bits = getHexValueForChar(idSequence.charAt(0)) << 28;
        bits |= getHexValueForChar(idSequence.charAt(1)) << 24;
        bits |= getHexValueForChar(idSequence.charAt(2)) << 20;
        bits |= getHexValueForChar(idSequence.charAt(3)) << 16;

        bits |= getHexValueForChar(idSequence.charAt(4)) << 12;
        bits |= getHexValueForChar(idSequence.charAt(5)) << 8;
        bits |= getHexValueForChar(idSequence.charAt(6)) << 4;
        bits |= getHexValueForChar(idSequence.charAt(7));

        return new LID<>(bits);
    }

    private static int getHexValueForChar(final char c) {
        try {
            if (HEX_VALUES[c] < 0) {
                throw new IllegalArgumentException("Illegal hexadecimal digit: " + c);
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Illegal hexadecimal digit: " + c, e);
        }

        return HEX_VALUES[c];
    }


    @SuppressWarnings("unchecked")
    public static <I extends IDAble> LID<I> cast(LID<? extends IDAble> id) {
        return (LID<I>) id;
    }

    public int toInteger() {
        return bits;
    }

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
        return Objects.hash(bits);
    }

    @Override
    public String toHexString() {
        final char[] idChars = new char[LID_STRING_LENGTH];

        idChars[0] = HEX_DIGITS[(bits & 0xf0000000) >>> 28];
        idChars[1] = HEX_DIGITS[(bits & 0x0f000000) >>> 24];
        idChars[2] = HEX_DIGITS[(bits & 0x00f00000) >>> 20];
        idChars[3] = HEX_DIGITS[(bits & 0x000f0000) >>> 16];
        idChars[4] = HEX_DIGITS[(bits & 0x0000f000) >>> 12];
        idChars[5] = HEX_DIGITS[(bits & 0x00000f00) >>> 8];
        idChars[6] = HEX_DIGITS[(bits & 0x000000f0) >>> 4];
        idChars[7] = HEX_DIGITS[bits & 0x0000000f];

        return new String(idChars);
    }
}
