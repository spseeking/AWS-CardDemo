package com.carddemo.batch.posting;

import com.carddemo.batch.copybook.FixedWidth;

/** CBTRN02C 2500-WRITE-REJECT-REC: the 350 byte daily record plus an 80 byte validation trailer. */
public final class RejectRecord {

    public static final int LENGTH = 430;

    private RejectRecord() {
    }

    public static String format(String dailyRecord, int reasonCode, String reasonDescription) {
        return FixedWidth.chars(dailyRecord, 350)
                + FixedWidth.digits(reasonCode, 4)
                + FixedWidth.chars(reasonDescription, 76);
    }
}
