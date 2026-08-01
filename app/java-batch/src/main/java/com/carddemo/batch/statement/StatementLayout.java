package com.carddemo.batch.statement;

import com.carddemo.batch.copybook.EditedNumeric;
import com.carddemo.batch.copybook.FixedWidth;

import java.math.BigDecimal;

/** The STATEMENT-LINES and HTML-LINES images of CBSTM03A. */
public final class StatementLayout {

    public static final String START_OF_STATEMENT = "*".repeat(31) + "START OF STATEMENT" + "*".repeat(31);
    public static final String END_OF_STATEMENT = "*".repeat(32) + "END OF STATEMENT" + "*".repeat(32);
    public static final String RULE = "-".repeat(80);

    private StatementLayout() {
    }

    public static String name(String value) {
        return FixedWidth.chars(value, 75) + " ".repeat(5);
    }

    public static String addressLine(String value) {
        return FixedWidth.chars(value, 50) + " ".repeat(30);
    }

    public static String addressLine3(String value) {
        return FixedWidth.chars(value, 80);
    }

    public static String centered(String label, int leftPad, int labelLength, int rightPad) {
        return " ".repeat(leftPad) + FixedWidth.chars(label, labelLength) + " ".repeat(rightPad);
    }

    public static String accountId(long value) {
        return FixedWidth.chars("Account ID         :", 20) + FixedWidth.chars(String.valueOf(value), 20)
                + " ".repeat(40);
    }

    public static String currentBalance(BigDecimal value) {
        return "Current Balance    :" + EditedNumeric.zeroFilledTrailingSign(value) + " ".repeat(47);
    }

    public static String ficoScore(int value) {
        return "FICO Score         :" + FixedWidth.chars(String.valueOf(value), 20) + " ".repeat(40);
    }

    public static String transactionHeader() {
        return FixedWidth.chars("Tran ID         ", 16) + FixedWidth.chars("Tran Details    ", 51)
                + "  Tran Amount";
    }

    public static String transaction(String id, String details, BigDecimal amount) {
        return FixedWidth.chars(id, 16) + " " + FixedWidth.chars(details, 49) + "$"
                + EditedNumeric.zeroSuppressedTrailingSign(amount);
    }

    public static String total(BigDecimal amount) {
        return "Total EXP:" + " ".repeat(56) + "$" + EditedNumeric.zeroSuppressedTrailingSign(amount);
    }
}
