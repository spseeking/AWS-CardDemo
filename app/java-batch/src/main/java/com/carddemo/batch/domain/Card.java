package com.carddemo.batch.domain;

import com.carddemo.batch.copybook.FixedWidth;

/** CARD-RECORD (copybook CVACT02Y, RECLN 150). */
public record Card(String cardNumber, long accountId, int cvvCode, String embossedName, String expirationDate,
                   String activeStatus, String filler) {

    public static final int LENGTH = 150;

    public static Card parse(String record) {
        return new Card(
                FixedWidth.alphanumeric(record, 0, 16),
                FixedWidth.number(record, 16, 11),
                (int) FixedWidth.number(record, 27, 3),
                FixedWidth.alphanumeric(record, 30, 50),
                FixedWidth.alphanumeric(record, 80, 10),
                FixedWidth.alphanumeric(record, 90, 1),
                FixedWidth.alphanumeric(record, 91, 59));
    }

    public String format() {
        return FixedWidth.chars(cardNumber, 16)
                + FixedWidth.digits(accountId, 11)
                + FixedWidth.digits(cvvCode, 3)
                + FixedWidth.chars(embossedName, 50)
                + FixedWidth.chars(expirationDate, 10)
                + FixedWidth.chars(activeStatus, 1)
                + FixedWidth.chars(filler, 59);
    }
}
