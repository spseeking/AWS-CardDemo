package com.carddemo.batch.domain;

import com.carddemo.batch.copybook.FixedWidth;
import com.carddemo.batch.copybook.ZonedDecimal;

import java.math.BigDecimal;

/** TRAN-CAT-BAL-RECORD (copybook CVTRA01Y, RECLN 50). */
public class TranCatBalance {

    public static final int LENGTH = 50;

    private long accountId;
    private String transactionTypeCode;
    private int transactionCategoryCode;
    private BigDecimal balance;
    private String filler = "";

    public TranCatBalance() {
    }

    public TranCatBalance(long accountId, String transactionTypeCode, int transactionCategoryCode,
                          BigDecimal balance) {
        this.accountId = accountId;
        this.transactionTypeCode = transactionTypeCode;
        this.transactionCategoryCode = transactionCategoryCode;
        this.balance = balance;
    }

    public static TranCatBalance parse(String record) {
        TranCatBalance balance = new TranCatBalance();
        balance.accountId = FixedWidth.number(record, 0, 11);
        balance.transactionTypeCode = FixedWidth.alphanumeric(record, 11, 2);
        balance.transactionCategoryCode = (int) FixedWidth.number(record, 13, 4);
        balance.balance = FixedWidth.zoned(record, 17, 11, 2);
        balance.filler = FixedWidth.alphanumeric(record, 28, 22);
        return balance;
    }

    public String format() {
        return FixedWidth.digits(accountId, 11)
                + FixedWidth.chars(transactionTypeCode, 2)
                + FixedWidth.digits(transactionCategoryCode, 4)
                + ZonedDecimal.format(balance, 11, 2)
                + FixedWidth.chars(filler, 22);
    }

    public String key() {
        return key(accountId, transactionTypeCode, transactionCategoryCode);
    }

    public static String key(long accountId, String transactionTypeCode, int transactionCategoryCode) {
        return FixedWidth.digits(accountId, 11)
                + FixedWidth.chars(transactionTypeCode, 2)
                + FixedWidth.digits(transactionCategoryCode, 4);
    }

    public long getAccountId() {
        return accountId;
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public int getTransactionCategoryCode() {
        return transactionCategoryCode;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    /** The CBTRN01C style DISPLAY of TRAN-CAT-BAL-RECORD. */
    @Override
    public String toString() {
        return "ACCT-ID=" + FixedWidth.digits(accountId, 11)
                + " TYPE-CD=" + transactionTypeCode
                + " CAT-CD=" + transactionCategoryCode
                + " BALANCE=" + balance;
    }
}
