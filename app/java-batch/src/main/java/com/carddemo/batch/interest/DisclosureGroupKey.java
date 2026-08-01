package com.carddemo.batch.interest;

import com.carddemo.batch.copybook.FixedWidth;

/** DIS-GROUP-KEY: account group id, transaction type code and transaction category code. */
public final class DisclosureGroupKey {

    private DisclosureGroupKey() {
    }

    public static String of(String accountGroupId, String transactionTypeCode, int transactionCategoryCode) {
        return FixedWidth.chars(accountGroupId, 10)
                + FixedWidth.chars(transactionTypeCode, 2)
                + FixedWidth.digits(transactionCategoryCode, 4);
    }
}
