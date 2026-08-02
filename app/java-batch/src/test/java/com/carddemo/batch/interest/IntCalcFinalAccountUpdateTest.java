package com.carddemo.batch.interest;

import com.carddemo.batch.domain.Account;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With legacy parity switched off the last account of the category balance file also receives its
 * accrued interest, which CBACT04C never does because its end-of-file branch is unreachable.
 */
@SpringBatchTest
@SpringBootTest(properties = "carddemo.legacy-skip-final-account-update=false")
class IntCalcFinalAccountUpdateTest {

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
    void runJob() throws Exception {
        BatchFixtureFiles.writeIntCalcData(DIRECTORY);
        jobLauncherTestUtils.setJob(intCalcJob);
        jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addString("parameterDate", "2022-06-30")
                .addLong("run", System.nanoTime())
                .toJobParameters());
    }

    @Test
    void postsInterestToTheFinalAccountAsWell() {
        Account account = BatchFixtureFiles.readLines(DIRECTORY.resolve("acctdata.txt")).stream()
                .map(Account::parse)
                .collect(Collectors.toMap(Account::getAccountId, Function.identity()))
                .get(12L);

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("210.00");
        assertThat(account.getCurrentCycleCredit()).isEqualByComparingTo("0.00");
    }
}
