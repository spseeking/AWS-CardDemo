package com.carddemo.batch.domain;

import com.carddemo.batch.copybook.FixedWidth;

/** CUSTOMER-RECORD (copybook CVCUS01Y, RECLN 500). */
public record Customer(long customerId, String firstName, String middleName, String lastName,
                       String addressLine1, String addressLine2, String addressLine3, String stateCode,
                       String countryCode, String zip, String phone1, String phone2, long ssn,
                       String governmentIssuedId, String dateOfBirth, String eftAccountId,
                       String primaryCardHolderIndicator, int ficoScore, String filler) {

    public static final int LENGTH = 500;

    public static Customer parse(String record) {
        return new Customer(
                FixedWidth.number(record, 0, 9),
                FixedWidth.alphanumeric(record, 9, 25),
                FixedWidth.alphanumeric(record, 34, 25),
                FixedWidth.alphanumeric(record, 59, 25),
                FixedWidth.alphanumeric(record, 84, 50),
                FixedWidth.alphanumeric(record, 134, 50),
                FixedWidth.alphanumeric(record, 184, 50),
                FixedWidth.alphanumeric(record, 234, 2),
                FixedWidth.alphanumeric(record, 236, 3),
                FixedWidth.alphanumeric(record, 239, 10),
                FixedWidth.alphanumeric(record, 249, 15),
                FixedWidth.alphanumeric(record, 264, 15),
                FixedWidth.number(record, 279, 9),
                FixedWidth.alphanumeric(record, 288, 20),
                FixedWidth.alphanumeric(record, 308, 10),
                FixedWidth.alphanumeric(record, 318, 10),
                FixedWidth.alphanumeric(record, 328, 1),
                (int) FixedWidth.number(record, 329, 3),
                FixedWidth.alphanumeric(record, 332, 168));
    }

    public String format() {
        return FixedWidth.digits(customerId, 9)
                + FixedWidth.chars(firstName, 25)
                + FixedWidth.chars(middleName, 25)
                + FixedWidth.chars(lastName, 25)
                + FixedWidth.chars(addressLine1, 50)
                + FixedWidth.chars(addressLine2, 50)
                + FixedWidth.chars(addressLine3, 50)
                + FixedWidth.chars(stateCode, 2)
                + FixedWidth.chars(countryCode, 3)
                + FixedWidth.chars(zip, 10)
                + FixedWidth.chars(phone1, 15)
                + FixedWidth.chars(phone2, 15)
                + FixedWidth.digits(ssn, 9)
                + FixedWidth.chars(governmentIssuedId, 20)
                + FixedWidth.chars(dateOfBirth, 10)
                + FixedWidth.chars(eftAccountId, 10)
                + FixedWidth.chars(primaryCardHolderIndicator, 1)
                + FixedWidth.digits(ficoScore, 3)
                + FixedWidth.chars(filler, 168);
    }

    public String fullName() {
        return (firstName.trim() + " " + middleName.trim() + " " + lastName.trim()).replaceAll("\\s+", " ").trim();
    }
}
