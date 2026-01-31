package dk.ceti.jdentifiers.id;

import java.util.Arrays;

abstract class VariableLengthID {
    static final byte[] HEX_DIGITS =
            new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    static final int[] HEX_VALUES = new int[128];

    static {
        Arrays.fill(HEX_VALUES, -1);

        HEX_VALUES['0'] = 0x0;
        HEX_VALUES['1'] = 0x1;
        HEX_VALUES['2'] = 0x2;
        HEX_VALUES['3'] = 0x3;
        HEX_VALUES['4'] = 0x4;
        HEX_VALUES['5'] = 0x5;
        HEX_VALUES['6'] = 0x6;
        HEX_VALUES['7'] = 0x7;
        HEX_VALUES['8'] = 0x8;
        HEX_VALUES['9'] = 0x9;

        HEX_VALUES['a'] = 0xa;
        HEX_VALUES['b'] = 0xb;
        HEX_VALUES['c'] = 0xc;
        HEX_VALUES['d'] = 0xd;
        HEX_VALUES['e'] = 0xe;
        HEX_VALUES['f'] = 0xf;

        HEX_VALUES['A'] = 0xa;
        HEX_VALUES['B'] = 0xb;
        HEX_VALUES['C'] = 0xc;
        HEX_VALUES['D'] = 0xd;
        HEX_VALUES['E'] = 0xe;
        HEX_VALUES['F'] = 0xf;
    }

    VariableLengthID() {
    }
}
