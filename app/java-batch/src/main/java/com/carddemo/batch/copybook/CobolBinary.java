package com.carddemo.batch.copybook;

/** COMP (binary) integers: big endian, sized from the digit count of the picture. */
public final class CobolBinary {

    private CobolBinary() {
    }

    /** Bytes IBM COBOL allocates for a {@code PIC 9(n) COMP} field. */
    public static int byteLength(int digits) {
        if (digits <= 4) {
            return 2;
        }
        return digits <= 9 ? 4 : 8;
    }

    public static long parse(byte[] record, int offset, int digits) {
        long value = 0;
        int length = byteLength(digits);
        for (int i = 0; i < length; i++) {
            value = (value << 8) | (record[offset + i] & 0xFFL);
        }
        int bits = length * 8;
        if (bits < 64 && (value & (1L << (bits - 1))) != 0) {
            value -= 1L << bits;
        }
        return value;
    }

    public static byte[] format(long value, int digits) {
        int length = byteLength(digits);
        byte[] bytes = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }
}
