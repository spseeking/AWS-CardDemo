package com.carddemo.batch.domain;

import com.carddemo.batch.copybook.FixedWidth;
import com.carddemo.batch.copybook.ZonedDecimal;

import java.math.BigDecimal;

/** ACCOUNT-RECORD (copybook CVACT01Y, RECLN 300). */
public class Account {

    public static final int LENGTH = 300;

    private long accountId;
    private String activeStatus;
    private BigDecimal currentBalance;
    private BigDecimal creditLimit;
    private BigDecimal cashCreditLimit;
    private String openDate;
    private String expirationDate;
    private String reissueDate;
    private BigDecimal currentCycleCredit;
    private BigDecimal currentCycleDebit;
    private String addressZip;
    private String groupId;
    private String filler = "";

    public static Account parse(String record) {
        Account account = new Account();
        account.accountId = FixedWidth.number(record, 0, 11);
        account.activeStatus = FixedWidth.alphanumeric(record, 11, 1);
        account.currentBalance = FixedWidth.zoned(record, 12, 12, 2);
        account.creditLimit = FixedWidth.zoned(record, 24, 12, 2);
        account.cashCreditLimit = FixedWidth.zoned(record, 36, 12, 2);
        account.openDate = FixedWidth.alphanumeric(record, 48, 10);
        account.expirationDate = FixedWidth.alphanumeric(record, 58, 10);
        account.reissueDate = FixedWidth.alphanumeric(record, 68, 10);
        account.currentCycleCredit = FixedWidth.zoned(record, 78, 12, 2);
        account.currentCycleDebit = FixedWidth.zoned(record, 90, 12, 2);
        account.addressZip = FixedWidth.alphanumeric(record, 102, 10);
        account.groupId = FixedWidth.alphanumeric(record, 112, 10);
        account.filler = FixedWidth.alphanumeric(record, 122, 178);
        return account;
    }

    public String format() {
        return FixedWidth.digits(accountId, 11)
                + FixedWidth.chars(activeStatus, 1)
                + ZonedDecimal.format(currentBalance, 12, 2)
                + ZonedDecimal.format(creditLimit, 12, 2)
                + ZonedDecimal.format(cashCreditLimit, 12, 2)
                + FixedWidth.chars(openDate, 10)
                + FixedWidth.chars(expirationDate, 10)
                + FixedWidth.chars(reissueDate, 10)
                + ZonedDecimal.format(currentCycleCredit, 12, 2)
                + ZonedDecimal.format(currentCycleDebit, 12, 2)
                + FixedWidth.chars(addressZip, 10)
                + FixedWidth.chars(groupId, 10)
                + FixedWidth.chars(filler, 178);
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit;
    }

    public String getOpenDate() {
        return openDate;
    }

    public void setOpenDate(String openDate) {
        this.openDate = openDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getReissueDate() {
        return reissueDate;
    }

    public void setReissueDate(String reissueDate) {
        this.reissueDate = reissueDate;
    }

    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }

    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = currentCycleCredit;
    }

    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }

    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit;
    }

    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getFiller() {
        return filler;
    }

    public void setFiller(String filler) {
        this.filler = filler;
    }
}
