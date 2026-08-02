package com.carddemo.batch.domain;

import com.carddemo.batch.copybook.FixedWidth;

/** TRAN-TYPE-RECORD (copybook CVTRA03Y, RECLN 60). */
public record TranType(String typeCode, String description, String filler) {

    public static final int LENGTH = 60;

    public static TranType parse(String record) {
        return new TranType(
                FixedWidth.alphanumeric(record, 0, 2),
                FixedWidth.alphanumeric(record, 2, 50),
                FixedWidth.alphanumeric(record, 52, 8));
    }

    public String format() {
        return FixedWidth.chars(typeCode, 2) + FixedWidth.chars(description, 50) + FixedWidth.chars(filler, 8);
    }
}
