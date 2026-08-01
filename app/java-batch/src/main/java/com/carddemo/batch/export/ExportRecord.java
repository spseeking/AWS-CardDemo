package com.carddemo.batch.export;

import com.carddemo.batch.copybook.CobolBinary;
import com.carddemo.batch.copybook.FixedWidth;
import com.carddemo.batch.copybook.PackedDecimal;
import com.carddemo.batch.copybook.ZonedDecimal;
import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.Card;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.Customer;
import com.carddemo.batch.domain.TransactionRecord;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Copybook CVEXPORT: the 500 byte multi record branch migration layout shared by CBEXPORT and
 * CBIMPORT. Unlike the master files these records mix DISPLAY, COMP and COMP-3 fields, so they
 * are handled as bytes rather than text.
 */
public final class ExportRecord {

    public static final int LENGTH = 500;
    public static final int DATA_OFFSET = 40;

    public static final char TYPE_CUSTOMER = 'C';
    public static final char TYPE_ACCOUNT = 'A';
    public static final char TYPE_XREF = 'X';
    public static final char TYPE_TRANSACTION = 'T';
    public static final char TYPE_CARD = 'D';

    private ExportRecord() {
    }

    public static byte[] header(char recordType, String timestamp, long sequenceNumber, String branchId,
                                String regionCode) {
        byte[] record = new byte[LENGTH];
        java.util.Arrays.fill(record, (byte) ' ');
        record[0] = (byte) recordType;
        text(record, 1, 26, timestamp);
        System.arraycopy(CobolBinary.format(sequenceNumber, 9), 0, record, 27, 4);
        text(record, 31, 4, branchId);
        text(record, 35, 5, regionCode);
        return record;
    }

    public static char recordType(byte[] record) {
        return (char) record[0];
    }

    public static String timestamp(byte[] record) {
        return text(record, 1, 26);
    }

    public static long sequenceNumber(byte[] record) {
        return CobolBinary.parse(record, 27, 9);
    }

    public static String branchId(byte[] record) {
        return text(record, 31, 4);
    }

    public static String regionCode(byte[] record) {
        return text(record, 35, 5);
    }

    public static void writeCustomer(byte[] record, Customer customer) {
        int base = DATA_OFFSET;
        binary(record, base, 9, customer.customerId());
        text(record, base + 4, 25, customer.firstName());
        text(record, base + 29, 25, customer.middleName());
        text(record, base + 54, 25, customer.lastName());
        text(record, base + 79, 50, customer.addressLine1());
        text(record, base + 129, 50, customer.addressLine2());
        text(record, base + 179, 50, customer.addressLine3());
        text(record, base + 229, 2, customer.stateCode());
        text(record, base + 231, 3, customer.countryCode());
        text(record, base + 234, 10, customer.zip());
        text(record, base + 244, 15, customer.phone1());
        text(record, base + 259, 15, customer.phone2());
        text(record, base + 274, 9, FixedWidth.digits(customer.ssn(), 9));
        text(record, base + 283, 20, customer.governmentIssuedId());
        text(record, base + 303, 10, customer.dateOfBirth());
        text(record, base + 313, 10, customer.eftAccountId());
        text(record, base + 323, 1, customer.primaryCardHolderIndicator());
        // EXP-CUST-FICO-CREDIT-SCORE is PIC 9(03) COMP-3, so its sign nibble is F, not C
        packedUnsigned(record, base + 324, 3, BigDecimal.valueOf(customer.ficoScore()));
        text(record, base + 326, 134, "");
    }

    public static Customer readCustomer(byte[] record) {
        int base = DATA_OFFSET;
        return new Customer(CobolBinary.parse(record, base, 9),
                text(record, base + 4, 25), text(record, base + 29, 25), text(record, base + 54, 25),
                text(record, base + 79, 50), text(record, base + 129, 50), text(record, base + 179, 50),
                text(record, base + 229, 2), text(record, base + 231, 3), text(record, base + 234, 10),
                text(record, base + 244, 15), text(record, base + 259, 15),
                Long.parseLong(text(record, base + 274, 9).trim()),
                text(record, base + 283, 20), text(record, base + 303, 10), text(record, base + 313, 10),
                text(record, base + 323, 1),
                PackedDecimal.parse(record, base + 324, 3, 0).intValue(),
                " ".repeat(168));
    }

    public static void writeAccount(byte[] record, Account account) {
        int base = DATA_OFFSET;
        text(record, base, 11, FixedWidth.digits(account.getAccountId(), 11));
        text(record, base + 11, 1, account.getActiveStatus());
        packed(record, base + 12, 12, 2, account.getCurrentBalance());
        text(record, base + 19, 12, ZonedDecimal.format(account.getCreditLimit(), 12, 2));
        packed(record, base + 31, 12, 2, account.getCashCreditLimit());
        text(record, base + 38, 10, account.getOpenDate());
        text(record, base + 48, 10, account.getExpirationDate());
        text(record, base + 58, 10, account.getReissueDate());
        text(record, base + 68, 12, ZonedDecimal.format(account.getCurrentCycleCredit(), 12, 2));
        binary(record, base + 80, 12, account.getCurrentCycleDebit().movePointRight(2).longValueExact());
        text(record, base + 88, 10, account.getAddressZip());
        text(record, base + 98, 10, account.getGroupId());
        text(record, base + 108, 352, "");
    }

    public static Account readAccount(byte[] record) {
        int base = DATA_OFFSET;
        Account account = new Account();
        account.setAccountId(Long.parseLong(text(record, base, 11).trim()));
        account.setActiveStatus(text(record, base + 11, 1));
        account.setCurrentBalance(PackedDecimal.parse(record, base + 12, 12, 2));
        account.setCreditLimit(ZonedDecimal.parse(text(record, base + 19, 12), 2));
        account.setCashCreditLimit(PackedDecimal.parse(record, base + 31, 12, 2));
        account.setOpenDate(text(record, base + 38, 10));
        account.setExpirationDate(text(record, base + 48, 10));
        account.setReissueDate(text(record, base + 58, 10));
        account.setCurrentCycleCredit(ZonedDecimal.parse(text(record, base + 68, 12), 2));
        account.setCurrentCycleDebit(BigDecimal.valueOf(CobolBinary.parse(record, base + 80, 12), 2));
        account.setAddressZip(text(record, base + 88, 10));
        account.setGroupId(text(record, base + 98, 10));
        account.setFiller(" ".repeat(178));
        return account;
    }

    public static void writeXref(byte[] record, CardXref xref) {
        int base = DATA_OFFSET;
        text(record, base, 16, xref.cardNumber());
        text(record, base + 16, 9, FixedWidth.digits(xref.customerId(), 9));
        binary(record, base + 25, 11, xref.accountId());
        text(record, base + 33, 427, "");
    }

    public static CardXref readXref(byte[] record) {
        int base = DATA_OFFSET;
        return new CardXref(text(record, base, 16),
                Long.parseLong(text(record, base + 16, 9).trim()),
                CobolBinary.parse(record, base + 25, 11),
                " ".repeat(14));
    }

    public static void writeTransaction(byte[] record, TransactionRecord transaction) {
        int base = DATA_OFFSET;
        text(record, base, 16, transaction.getId());
        text(record, base + 16, 2, transaction.getTypeCode());
        text(record, base + 18, 4, FixedWidth.digits(transaction.getCategoryCode(), 4));
        text(record, base + 22, 10, transaction.getSource());
        text(record, base + 32, 100, transaction.getDescription());
        packed(record, base + 132, 11, 2, transaction.getAmount());
        binary(record, base + 138, 9, transaction.getMerchantId());
        text(record, base + 142, 50, transaction.getMerchantName());
        text(record, base + 192, 50, transaction.getMerchantCity());
        text(record, base + 242, 10, transaction.getMerchantZip());
        text(record, base + 252, 16, transaction.getCardNumber());
        text(record, base + 268, 26, transaction.getOriginTimestamp());
        text(record, base + 294, 26, transaction.getProcessTimestamp());
        text(record, base + 320, 140, "");
    }

    public static TransactionRecord readTransaction(byte[] record) {
        int base = DATA_OFFSET;
        TransactionRecord transaction = new TransactionRecord();
        transaction.setId(text(record, base, 16));
        transaction.setTypeCode(text(record, base + 16, 2));
        transaction.setCategoryCode(Integer.parseInt(text(record, base + 18, 4).trim()));
        transaction.setSource(text(record, base + 22, 10));
        transaction.setDescription(text(record, base + 32, 100));
        transaction.setAmount(PackedDecimal.parse(record, base + 132, 11, 2));
        transaction.setMerchantId(CobolBinary.parse(record, base + 138, 9));
        transaction.setMerchantName(text(record, base + 142, 50));
        transaction.setMerchantCity(text(record, base + 192, 50));
        transaction.setMerchantZip(text(record, base + 242, 10));
        transaction.setCardNumber(text(record, base + 252, 16));
        transaction.setOriginTimestamp(text(record, base + 268, 26));
        transaction.setProcessTimestamp(text(record, base + 294, 26));
        transaction.setFiller(" ".repeat(20));
        return transaction;
    }

    public static void writeCard(byte[] record, Card card) {
        int base = DATA_OFFSET;
        text(record, base, 16, card.cardNumber());
        binary(record, base + 16, 11, card.accountId());
        binary(record, base + 24, 3, card.cvvCode());
        text(record, base + 26, 50, card.embossedName());
        text(record, base + 76, 10, card.expirationDate());
        text(record, base + 86, 1, card.activeStatus());
        text(record, base + 87, 373, "");
    }

    public static Card readCard(byte[] record) {
        int base = DATA_OFFSET;
        return new Card(text(record, base, 16),
                CobolBinary.parse(record, base + 16, 11),
                (int) CobolBinary.parse(record, base + 24, 3),
                text(record, base + 26, 50),
                text(record, base + 76, 10),
                text(record, base + 86, 1),
                " ".repeat(59));
    }

    private static void text(byte[] record, int offset, int length, String value) {
        byte[] bytes = FixedWidth.chars(value, length).getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(bytes, 0, record, offset, length);
    }

    private static String text(byte[] record, int offset, int length) {
        return new String(record, offset, length, StandardCharsets.ISO_8859_1);
    }

    private static void binary(byte[] record, int offset, int digits, long value) {
        byte[] bytes = CobolBinary.format(value, digits);
        System.arraycopy(bytes, 0, record, offset, bytes.length);
    }

    private static void packed(byte[] record, int offset, int digits, int scale, BigDecimal value) {
        byte[] bytes = PackedDecimal.format(value, digits, scale);
        System.arraycopy(bytes, 0, record, offset, bytes.length);
    }

    private static void packedUnsigned(byte[] record, int offset, int digits, BigDecimal value) {
        byte[] bytes = PackedDecimal.formatUnsigned(value, digits, 0);
        System.arraycopy(bytes, 0, record, offset, bytes.length);
    }
}
