package com.carddemo.batch.copybook;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/** COMP-3 (packed decimal): two digits per byte, sign nibble last (C positive, D negative, F unsigned). */
public final class PackedDecimal {

    private PackedDecimal() {
    }

    /** Bytes needed for a {@code PIC 9(n)} or {@code PIC S9(n)V9(m)} COMP-3 field. */
    public static int byteLength(int digits) {
        return digits / 2 + 1;
    }

    public static BigDecimal parse(byte[] record, int offset, int digits, int scale) {
        int length = byteLength(digits);
        StringBuilder text = new StringBuilder(digits + 1);
        for (int i = 0; i < length; i++) {
            int value = record[offset + i] & 0xFF;
            text.append((char) ('0' + (value >>> 4)));
            if (i < length - 1) {
                text.append((char) ('0' + (value & 0x0F)));
            }
        }
        int sign = record[offset + length - 1] & 0x0F;
        BigDecimal unscaled = new BigDecimal(new BigInteger(text.toString()), scale);
        return sign == 0x0D ? unscaled.negate() : unscaled;
    }

    /** A {@code PIC S9(n)} field: the sign nibble is C or D. */
    public static byte[] format(BigDecimal value, int digits, int scale) {
        return format(value, digits, scale, true);
    }

    /** A {@code PIC 9(n)} field: IBM COBOL stores the unsigned sign nibble F. */
    public static byte[] formatUnsigned(BigDecimal value, int digits, int scale) {
        return format(value, digits, scale, false);
    }

    private static byte[] format(BigDecimal value, int digits, int scale, boolean signed) {
        BigDecimal scaled = value.setScale(scale, RoundingMode.DOWN);
        String text = scaled.abs().unscaledValue().toString();
        if (text.length() > digits) {
            text = text.substring(text.length() - digits);
        }
        text = "0".repeat(digits - text.length()) + text;

        int length = byteLength(digits);
        byte[] packed = new byte[length];
        // digits is even for a field whose byte count includes a leading unused high nibble
        String nibbles = (digits % 2 == 0 ? "0" : "") + text;
        for (int i = 0; i < length; i++) {
            int high = nibbles.charAt(i * 2) - '0';
            int sign = signed ? (scaled.signum() < 0 ? 0x0D : 0x0C) : 0x0F;
            int low = i < length - 1 ? nibbles.charAt(i * 2 + 1) - '0' : sign;
            packed[i] = (byte) ((high << 4) | low);
        }
        return packed;
    }
}
