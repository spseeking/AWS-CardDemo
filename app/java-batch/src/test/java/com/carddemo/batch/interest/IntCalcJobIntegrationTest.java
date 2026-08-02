package com.carddemo.batch.interest;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.testsupport.BatchFixtureFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** INTCALC end to end: interest transactions and the account balances they roll up into. */
@SpringBatchTest
@SpringBootTest(properties = "carddemo.legacy-skip-final-account-update=true")
class IntCalcJobIntegrationTest {

    private static final Path DIRECTORY = BatchFixtureFiles.createDirectory();

    @DynamicPropertySource
    static void files(DynamicPropertyRegistry registry) {
        registry.add("carddemo.card-xref-file", () -> DIRECTORY.resolve("cardxref.txt"));
        registry.add("carddemo.account-file", () -> DIRECTORY.resolve("acctdata.txt"));
        registry.add("carddemo.tran-cat-balance-file", () -> DIRECTORY.resolve("tcatbal.txt"));
        registry.add("carddemo.disclosure-group-file", () -> DIRECTORY.resolve("discgrp.txt"));
        registry.add("carddemo.transaction-file", () -> DIRECTORY.resolve("transact.txt"));
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job intCalcJob;

    @BeforeEach
    void writeFixtures() throws Exception {
        BatchFixtureFiles.writeIntCalcData(DIRECTORY);
        jobLauncherTestUtils.setJob(intCalcJob);
        jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addString("parameterDate", "2022-06-30")
                .addLong("run", System.nanoTime())
                .toJobParameters());
    }

    @Test
    void writesOneInterestTransactionPerRatedCategoryBalance() {
        List<TransactionRecord> interest = BatchFixtureFiles.readLines(DIRECTORY.resolve("transact.txt")).stream()
                .map(TransactionRecord::parse)
                .toList();

        assertThat(interest).hasSize(2);

        TransactionRecord first = interest.get(0);
        assertThat(first.getId()).isEqualTo("2022-06-30000001");
        assertThat(first.getTypeCode()).isEqualTo("01");
        assertThat(first.getCategoryCode()).isEqualTo(5);
        assertThat(first.getSource().trim()).isEqualTo("System");
        assertThat(first.getDescription().trim()).isEqualTo("Int. for a/c 00000000011");
        assertThat(first.getCardNumber()).isEqualTo(BatchFixtureFiles.CARD_A);
        assertThat(first.getAmount()).as("1000.00 at 15.00% -> 12.50").isEqualByComparingTo("12.50");

        TransactionRecord second = interest.get(1);
        assertThat(second.getId()).isEqualTo("2022-06-30000002");
        assertThat(second.getAmount()).as("DEFAULT group 12.00% applies to the unknown group")
                .isEqualByComparingTo("10.00");
    }

    @Test
    void rollsAccruedInterestIntoTheAccountAndClearsTheCycleTotals() {
        Account account = accounts().get(11L);

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("112.50");
        assertThat(account.getCurrentCycleCredit()).isEqualByComparingTo("0.00");
        assertThat(account.getCurrentCycleDebit()).isEqualByComparingTo("0.00");
    }

    @Test
    void leavesTheFinalAccountUntouchedWhileLegacyParityIsEnabled() {
        Account account = accounts().get(12L);

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("200.00");
        assertThat(account.getCurrentCycleCredit()).isEqualByComparingTo("7.00");
    }

    private java.util.Map<Long, Account> accounts() {
        return BatchFixtureFiles.readLines(DIRECTORY.resolve("acctdata.txt")).stream()
                .map(Account::parse)
                .collect(Collectors.toMap(Account::getAccountId, Function.identity()));
    }
}
