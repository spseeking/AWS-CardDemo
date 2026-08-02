package com.carddemo.batch.domain;

import com.carddemo.batch.copybook.FixedWidth;

/** CARD-XREF-RECORD (copybook CVACT03Y, RECLN 50). */
public record CardXref(String cardNumber, long customerId, long accountId, String filler) {

    public static final int LENGTH = 50;

    public static CardXref parse(String record) {
        return new CardXref(
                FixedWidth.alphanumeric(record, 0, 16),
                FixedWidth.number(record, 16, 9),
                FixedWidth.number(record, 25, 11),
                FixedWidth.alphanumeric(record, 36, 14));
    }

    public String format() {
        return FixedWidth.chars(cardNumber, 16)
                + FixedWidth.digits(customerId, 9)
                + FixedWidth.digits(accountId, 11)
                + FixedWidth.chars(filler, 14);
    }
}
