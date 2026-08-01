package com.carddemo.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/** File assignments that replace the DD statements of the POSTTRAN and INTCALC JCL. */
@ConfigurationProperties(prefix = "carddemo")
public class BatchFilesProperties {

    /** DALYTRAN: daily transaction input (350 bytes). */
    private Path dailyTransactionFile = Path.of("data/dailytran.txt");
    /** DALYREJS: rejected daily transactions with validation trailer (430 bytes). */
    private Path dailyRejectFile = Path.of("target/data/dalyrejs.txt");
    /** TRANFILE: transaction master output (350 bytes). */
    private Path transactionFile = Path.of("target/data/transact.txt");
    /** XREFFILE: card / customer / account cross reference (50 bytes). */
    private Path cardXrefFile = Path.of("data/cardxref.txt");
    /** ACCTFILE: account master (300 bytes). */
    private Path accountFile = Path.of("data/acctdata.txt");
    /** TCATBALF: transaction category balances (50 bytes). */
    private Path tranCatBalanceFile = Path.of("data/tcatbal.txt");
    /** DISCGRP: disclosure groups holding the interest rates (50 bytes). */
    private Path disclosureGroupFile = Path.of("data/discgrp.txt");

    /**
     * CBACT04C never updates the account of the last transaction category balance group because
     * its end-of-file branch is unreachable. Leave this true to reproduce the legacy output
     * byte-for-byte; set it to false to also post the final account's accrued interest.
     */
    private boolean legacySkipFinalAccountUpdate = true;

    public Path getDailyTransactionFile() {
        return dailyTransactionFile;
    }

    public void setDailyTransactionFile(Path dailyTransactionFile) {
        this.dailyTransactionFile = dailyTransactionFile;
    }

    public Path getDailyRejectFile() {
        return dailyRejectFile;
    }

    public void setDailyRejectFile(Path dailyRejectFile) {
        this.dailyRejectFile = dailyRejectFile;
    }

    public Path getTransactionFile() {
        return transactionFile;
    }

    public void setTransactionFile(Path transactionFile) {
        this.transactionFile = transactionFile;
    }

    public Path getCardXrefFile() {
        return cardXrefFile;
    }

    public void setCardXrefFile(Path cardXrefFile) {
        this.cardXrefFile = cardXrefFile;
    }

    public Path getAccountFile() {
        return accountFile;
    }

    public void setAccountFile(Path accountFile) {
        this.accountFile = accountFile;
    }

    public Path getTranCatBalanceFile() {
        return tranCatBalanceFile;
    }

    public void setTranCatBalanceFile(Path tranCatBalanceFile) {
        this.tranCatBalanceFile = tranCatBalanceFile;
    }

    public Path getDisclosureGroupFile() {
        return disclosureGroupFile;
    }

    public void setDisclosureGroupFile(Path disclosureGroupFile) {
        this.disclosureGroupFile = disclosureGroupFile;
    }

    public boolean isLegacySkipFinalAccountUpdate() {
        return legacySkipFinalAccountUpdate;
    }

    public void setLegacySkipFinalAccountUpdate(boolean legacySkipFinalAccountUpdate) {
        this.legacySkipFinalAccountUpdate = legacySkipFinalAccountUpdate;
    }
}
