package com.carddemo.batch.domain;

import com.carddemo.batch.copybook.FixedWidth;

/** SEC-USER-DATA (copybook CSUSR01Y, RECLN 80). Type {@code A} is an administrator. */
public record SecurityUser(String userId, String firstName, String lastName, String password, String type,
                           String filler) {

    public static final int LENGTH = 80;
    public static final String TYPE_ADMIN = "A";
    public static final String TYPE_USER = "U";

    public static SecurityUser parse(String record) {
        return new SecurityUser(
                FixedWidth.alphanumeric(record, 0, 8),
                FixedWidth.alphanumeric(record, 8, 20),
                FixedWidth.alphanumeric(record, 28, 20),
                FixedWidth.alphanumeric(record, 48, 8),
                FixedWidth.alphanumeric(record, 56, 1),
                FixedWidth.alphanumeric(record, 57, 23));
    }

    public String format() {
        return FixedWidth.chars(userId, 8)
                + FixedWidth.chars(firstName, 20)
                + FixedWidth.chars(lastName, 20)
                + FixedWidth.chars(password, 8)
                + FixedWidth.chars(type, 1)
                + FixedWidth.chars(filler, 23);
    }

    public boolean isAdmin() {
        return TYPE_ADMIN.equalsIgnoreCase(type.trim());
    }
}
