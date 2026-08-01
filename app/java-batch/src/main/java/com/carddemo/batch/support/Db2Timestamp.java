package com.carddemo.batch.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Z-GET-DB2-FORMAT-TIMESTAMP: {@code yyyy-MM-dd HH:mm:ss.SSSSSS} in 26 characters. */
public final class Db2Timestamp {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private Db2Timestamp() {
    }

    public static String now() {
        return of(LocalDateTime.now());
    }

    public static String of(LocalDateTime moment) {
        return FORMAT.format(moment);
    }
}
