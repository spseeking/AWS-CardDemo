package com.carddemo.batch.copybook;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** COBOL numeric edited pictures used by the report and statement programs. */
public final class EditedNumeric {

    private static final String GROUPED_PATTERN = "###,###,##0.00";

    private EditedNumeric() {
    }

    /** {@code PIC -ZZZ,ZZZ,ZZZ.ZZ}: 15 characters, minus sign only when negative. */
    public static String signedGrouped(BigDecimal value) {
        return sign(value, false) + grouped(value);
    }

    /** {@code PIC +ZZZ,ZZZ,ZZZ.ZZ}: 15 characters, sign always printed. */
    public static String plusSignedGrouped(BigDecimal value) {
        return sign(value, true) + grouped(value);
    }

    /** {@code PIC Z(9).99-}: 13 characters, blank-suppressed integer part, trailing sign. */
    public static String zeroSuppressedTrailingSign(BigDecimal value) {
        BigDecimal scaled = value.setScale(2, RoundingMode.DOWN);
        String digits = scaled.abs().toPlainString();
        String integerPart = digits.substring(0, digits.indexOf('.'));
        String decimals = digits.substring(digits.indexOf('.') + 1);
        if (integerPart.equals("0")) {
            integerPart = "";
        }
        return " ".repeat(Math.max(0, 9 - integerPart.length())) + integerPart + "." + decimals
                + (scaled.signum() < 0 ? "-" : " ");
    }

    /** {@code PIC 9(9).99-}: 13 characters, zero-filled integer part, trailing sign. */
    public static String zeroFilledTrailingSign(BigDecimal value) {
        BigDecimal scaled = value.setScale(2, RoundingMode.DOWN);
        String digits = scaled.abs().toPlainString();
        String integerPart = digits.substring(0, digits.indexOf('.'));
        String decimals = digits.substring(digits.indexOf('.') + 1);
        if (integerPart.length() > 9) {
            integerPart = integerPart.substring(integerPart.length() - 9);
        }
        return "0".repeat(9 - integerPart.length()) + integerPart + "." + decimals
                + (scaled.signum() < 0 ? "-" : " ");
    }

    private static String sign(BigDecimal value, boolean alwaysSign) {
        if (value.signum() < 0) {
            return "-";
        }
        return alwaysSign ? "+" : " ";
    }

    private static String grouped(BigDecimal value) {
        DecimalFormat format = new DecimalFormat(GROUPED_PATTERN, DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.DOWN);
        String text = format.format(value.abs());
        return " ".repeat(Math.max(0, 14 - text.length())) + text;
    }
}
