package com.carddemo.batch.copybook;

import java.math.BigDecimal;

/** Helpers for reading and writing fixed-width copybook records. */
public final class FixedWidth {

    private FixedWidth() {
    }

    public static String pad(String record, int length) {
        if (record.length() >= length) {
            return record.substring(0, length);
        }
        return record + " ".repeat(length - record.length());
    }

    public static String alphanumeric(String record, int offset, int length) {
        return pad(record, offset + length).substring(offset, offset + length);
    }

    public static String text(String record, int offset, int length) {
        return alphanumeric(record, offset, length).trim();
    }

    public static long number(String record, int offset, int length) {
        String digits = text(record, offset, length);
        return digits.isEmpty() ? 0L : Long.parseLong(digits);
    }

    public static BigDecimal zoned(String record, int offset, int length, int scale) {
        return ZonedDecimal.parse(alphanumeric(record, offset, length), scale);
    }

    public static String chars(String value, int length) {
        String text = value == null ? "" : value;
        return pad(text, length);
    }

    public static String digits(long value, int length) {
        String text = Long.toString(value);
        if (text.length() > length) {
            text = text.substring(text.length() - length);
        }
        return "0".repeat(length - text.length()) + text;
    }
}
