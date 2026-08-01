package com.carddemo.batch.report;

import com.carddemo.batch.copybook.EditedNumeric;
import com.carddemo.batch.copybook.FixedWidth;

import java.math.BigDecimal;

/** The printed line images of copybook CVTRA07Y, padded to the 133 byte report record. */
public final class TransactionReportLayout {

    public static final int LINE_LENGTH = 133;
    public static final String SEPARATOR = "-".repeat(LINE_LENGTH);
    public static final String BLANK = " ".repeat(LINE_LENGTH);

    public static final String COLUMN_HEADER = FixedWidth.pad(
            FixedWidth.chars("Transaction ID", 17)
                    + FixedWidth.chars("Account ID", 12)
                    + FixedWidth.chars("Transaction Type", 19)
                    + FixedWidth.chars("Tran Category", 35)
                    + FixedWidth.chars("Tran Source", 14)
                    + " "
                    + FixedWidth.chars("        Amount", 16), LINE_LENGTH);

    private TransactionReportLayout() {
    }

    public static String nameHeader(String startDate, String endDate) {
        return FixedWidth.pad(FixedWidth.chars("DALYREPT", 38)
                + FixedWidth.chars("Daily Transaction Report", 41)
                + "Date Range: "
                + FixedWidth.chars(startDate, 10)
                + " to "
                + FixedWidth.chars(endDate, 10), LINE_LENGTH);
    }

    public static String detail(String transactionId, long accountId, String typeCode, String typeDescription,
                                int categoryCode, String categoryDescription, String source, BigDecimal amount) {
        return FixedWidth.pad(FixedWidth.chars(transactionId, 16)
                + " "
                + FixedWidth.digits(accountId, 11)
                + " "
                + FixedWidth.chars(typeCode, 2)
                + "-"
                + FixedWidth.chars(typeDescription, 15)
                + " "
                + FixedWidth.digits(categoryCode, 4)
                + "-"
                + FixedWidth.chars(categoryDescription, 29)
                + " "
                + FixedWidth.chars(source, 10)
                + "    "
                + EditedNumeric.signedGrouped(amount)
                + "  ", LINE_LENGTH);
    }

    public static String pageTotal(BigDecimal total) {
        return total("Page Total", 11, 86, total);
    }

    public static String accountTotal(BigDecimal total) {
        return total("Account Total", 13, 84, total);
    }

    public static String grandTotal(BigDecimal total) {
        return total("Grand Total", 11, 86, total);
    }

    private static String total(String label, int labelLength, int dotLength, BigDecimal total) {
        return FixedWidth.pad(FixedWidth.chars(label, labelLength)
                + ".".repeat(dotLength)
                + EditedNumeric.plusSignedGrouped(total), LINE_LENGTH);
    }
}
