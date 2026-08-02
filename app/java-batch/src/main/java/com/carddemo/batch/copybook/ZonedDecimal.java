package com.carddemo.batch.copybook;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Signed zoned decimal (COBOL {@code PIC S9(n)V9(s)} DISPLAY) codec.
 *
 * <p>The sign is carried as an overpunch on the last digit: {@code {} and {@code A}-{@code I}
 * denote a positive 0-9, {@code }} and {@code J}-{@code R} a negative 0-9. Unsigned
 * {@code PIC 9} fields are plain digits and are also accepted.
 */
public final class ZonedDecimal {

    private static final String POSITIVE_OVERPUNCH = "{ABCDEFGHI";
    private static final String NEGATIVE_OVERPUNCH = "}JKLMNOPQR";

    private ZonedDecimal() {
    }

    public static BigDecimal parse(String field, int scale) {
        String digits = field.trim();
        if (digits.isEmpty()) {
            return BigDecimal.ZERO.setScale(scale);
        }
        char last = digits.charAt(digits.length() - 1);
        String head = digits.substring(0, digits.length() - 1);
        boolean negative = false;
        char lastDigit;

        int positive = POSITIVE_OVERPUNCH.indexOf(last);
        int negativeIndex = NEGATIVE_OVERPUNCH.indexOf(last);
        if (positive >= 0) {
            lastDigit = (char) ('0' + positive);
        } else if (negativeIndex >= 0) {
            lastDigit = (char) ('0' + negativeIndex);
            negative = true;
        } else if (Character.isDigit(last)) {
            lastDigit = last;
        } else {
            throw new IllegalArgumentException("Not a zoned decimal field: '" + field + "'");
        }

        BigDecimal value = new BigDecimal(new BigInteger(head + lastDigit), scale);
        return negative ? value.negate() : value;
    }

    public static String format(BigDecimal value, int length, int scale) {
        BigDecimal scaled = value.setScale(scale, RoundingMode.DOWN);
        boolean negative = scaled.signum() < 0;
        String digits = scaled.abs().unscaledValue().toString();
        if (digits.length() > length) {
            // COBOL MOVE truncates high-order digits that do not fit the receiving field.
            digits = digits.substring(digits.length() - length);
        }
        digits = "0".repeat(length - digits.length()) + digits;

        int lastDigit = digits.charAt(length - 1) - '0';
        char overpunch = negative ? NEGATIVE_OVERPUNCH.charAt(lastDigit) : POSITIVE_OVERPUNCH.charAt(lastDigit);
        return digits.substring(0, length - 1) + overpunch;
    }
}
