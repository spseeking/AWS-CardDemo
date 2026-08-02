package com.carddemo.batch.domain;

import com.carddemo.batch.copybook.FixedWidth;

/** TRAN-CAT-RECORD (copybook CVTRA04Y, RECLN 60). */
public record TranCategory(String typeCode, int categoryCode, String description, String filler) {

    public static final int LENGTH = 60;

    public static TranCategory parse(String record) {
        return new TranCategory(
                FixedWidth.alphanumeric(record, 0, 2),
                (int) FixedWidth.number(record, 2, 4),
                FixedWidth.alphanumeric(record, 6, 50),
                FixedWidth.alphanumeric(record, 56, 4));
    }

    public String format() {
        return FixedWidth.chars(typeCode, 2)
                + FixedWidth.digits(categoryCode, 4)
                + FixedWidth.chars(description, 50)
                + FixedWidth.chars(filler, 4);
    }

    public String key() {
        return key(typeCode, categoryCode);
    }

    public static String key(String typeCode, int categoryCode) {
        return FixedWidth.chars(typeCode, 2) + FixedWidth.digits(categoryCode, 4);
    }
}
