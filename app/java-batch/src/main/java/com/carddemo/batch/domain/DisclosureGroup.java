package com.carddemo.batch.domain;

import com.carddemo.batch.copybook.FixedWidth;
import com.carddemo.batch.copybook.ZonedDecimal;

import java.math.BigDecimal;

/** DIS-GROUP-RECORD (copybook CVTRA02Y, RECLN 50). The interest rate is an annual percentage. */
public record DisclosureGroup(String accountGroupId, String transactionTypeCode, int transactionCategoryCode,
                              BigDecimal interestRate, String filler) {

    public static final int LENGTH = 50;

    /** Group id used by CBACT04C when no rate exists for the account's own group. */
    public static final String DEFAULT_GROUP_ID = "DEFAULT";

    public static DisclosureGroup parse(String record) {
        return new DisclosureGroup(
                FixedWidth.alphanumeric(record, 0, 10),
                FixedWidth.alphanumeric(record, 10, 2),
                (int) FixedWidth.number(record, 12, 4),
                FixedWidth.zoned(record, 16, 6, 2),
                FixedWidth.alphanumeric(record, 22, 28));
    }

    public String format() {
        return FixedWidth.chars(accountGroupId, 10)
                + FixedWidth.chars(transactionTypeCode, 2)
                + FixedWidth.digits(transactionCategoryCode, 4)
                + ZonedDecimal.format(interestRate, 6, 2)
                + FixedWidth.chars(filler, 28);
    }
}
