package com.carddemo.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/** File assignments that replace the DD statements of the converted CardDemo batch JCL. */
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
    /** CUSTFILE: customer master (500 bytes). */
    private Path customerFile = Path.of("data/custdata.txt");
    /** CARDFILE: card master (150 bytes). */
    private Path cardFile = Path.of("data/carddata.txt");
    /** TRANTYPE: transaction types (60 bytes). */
    private Path tranTypeFile = Path.of("data/trantype.txt");
    /** TRANCATG: transaction categories (60 bytes). */
    private Path tranCategoryFile = Path.of("data/trancatg.txt");
    /** DATEPARM: reporting date range, {@code yyyy-MM-dd yyyy-MM-dd}. */
    private Path dateParmFile = Path.of("data/dateparm.txt");
    /** TRANREPT: printed transaction detail report (133 byte lines). */
    private Path transactionReportFile = Path.of("target/data/tranrept.txt");
    /** STMTFILE: plain text account statements. */
    private Path statementFile = Path.of("target/data/stmtfile.txt");
    /** HTMLFILE: HTML account statements. */
    private Path statementHtmlFile = Path.of("target/data/stmtfile.html");
    /** EXPFILE: branch migration export (500 byte binary records). */
    private Path exportFile = Path.of("target/data/expfile.dat");
    /** Directory the import job writes the reconstructed master files into. */
    private Path importDirectory = Path.of("target/data/import");
    /** USRSEC: security users backing the online sign on and user administration screens (80 bytes). */
    private Path userSecurityFile = Path.of("data/usrsec.txt");
    /**
     * Hashed sign on credentials. USRSEC only has room for the legacy 8 byte clear password, so the
     * hashes live beside it and take precedence; a user still on the legacy field is migrated on the
     * next successful sign on.
     */
    private Path userCredentialFile = Path.of("data/usrsec.hash");

    /**
     * CBACT04C never updates the account of the last transaction category balance group because
     * its end-of-file branch is unreachable. Leave this true to reproduce the legacy output
     * byte-for-byte; set it to false to also post the final account's accrued interest.
     */
    private boolean legacySkipFinalAccountUpdate = true;

    /**
     * CBTRN03C filters on the reporting date range with a NEXT SENTENCE that leaves the whole
     * PERFORM UNTIL sentence, so the mainframe report stops at the first out of range record
     * instead of skipping it. The conversion skips by default; set this true for legacy output.
     */
    private boolean legacyStopReportAtOutOfRangeRecord = false;

    /**
     * At end of file CBTRN03C adds TRAN-AMT to the page and account totals once more from the
     * record area, double counting the last reported transaction. True reproduces that.
     */
    private boolean legacyReportDoubleCountsLastAmount = true;

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

    public Path getCustomerFile() {
        return customerFile;
    }

    public void setCustomerFile(Path customerFile) {
        this.customerFile = customerFile;
    }

    public Path getCardFile() {
        return cardFile;
    }

    public void setCardFile(Path cardFile) {
        this.cardFile = cardFile;
    }

    public Path getTranTypeFile() {
        return tranTypeFile;
    }

    public void setTranTypeFile(Path tranTypeFile) {
        this.tranTypeFile = tranTypeFile;
    }

    public Path getTranCategoryFile() {
        return tranCategoryFile;
    }

    public void setTranCategoryFile(Path tranCategoryFile) {
        this.tranCategoryFile = tranCategoryFile;
    }

    public Path getDateParmFile() {
        return dateParmFile;
    }

    public void setDateParmFile(Path dateParmFile) {
        this.dateParmFile = dateParmFile;
    }

    public Path getTransactionReportFile() {
        return transactionReportFile;
    }

    public void setTransactionReportFile(Path transactionReportFile) {
        this.transactionReportFile = transactionReportFile;
    }

    public Path getStatementFile() {
        return statementFile;
    }

    public void setStatementFile(Path statementFile) {
        this.statementFile = statementFile;
    }

    public Path getStatementHtmlFile() {
        return statementHtmlFile;
    }

    public void setStatementHtmlFile(Path statementHtmlFile) {
        this.statementHtmlFile = statementHtmlFile;
    }

    public Path getExportFile() {
        return exportFile;
    }

    public void setExportFile(Path exportFile) {
        this.exportFile = exportFile;
    }

    public Path getImportDirectory() {
        return importDirectory;
    }

    public void setImportDirectory(Path importDirectory) {
        this.importDirectory = importDirectory;
    }

    public Path getUserSecurityFile() {
        return userSecurityFile;
    }

    public void setUserSecurityFile(Path userSecurityFile) {
        this.userSecurityFile = userSecurityFile;
    }

    public Path getUserCredentialFile() {
        return userCredentialFile;
    }

    public void setUserCredentialFile(Path userCredentialFile) {
        this.userCredentialFile = userCredentialFile;
    }

    public boolean isLegacySkipFinalAccountUpdate() {
        return legacySkipFinalAccountUpdate;
    }

    public void setLegacySkipFinalAccountUpdate(boolean legacySkipFinalAccountUpdate) {
        this.legacySkipFinalAccountUpdate = legacySkipFinalAccountUpdate;
    }

    public boolean isLegacyStopReportAtOutOfRangeRecord() {
        return legacyStopReportAtOutOfRangeRecord;
    }

    public void setLegacyStopReportAtOutOfRangeRecord(boolean legacyStopReportAtOutOfRangeRecord) {
        this.legacyStopReportAtOutOfRangeRecord = legacyStopReportAtOutOfRangeRecord;
    }

    public boolean isLegacyReportDoubleCountsLastAmount() {
        return legacyReportDoubleCountsLastAmount;
    }

    public void setLegacyReportDoubleCountsLastAmount(boolean legacyReportDoubleCountsLastAmount) {
        this.legacyReportDoubleCountsLastAmount = legacyReportDoubleCountsLastAmount;
    }
}
