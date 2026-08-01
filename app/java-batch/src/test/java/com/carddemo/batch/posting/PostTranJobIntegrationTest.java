package com.carddemo.batch.posting;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.TranCatBalance;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.testsupport.BatchFixtureFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** POSTTRAN end to end against fixed-width fixtures. */
@SpringBatchTest
@SpringBootTest
class PostTranJobIntegrationTest {

    private static final Path DIRECTORY = BatchFixtureFiles.createDirectory();

    @DynamicPropertySource
    static void files(DynamicPropertyRegistry registry) {
        registry.add("carddemo.daily-transaction-file", () -> DIRECTORY.resolve("dailytran.txt"));
        registry.add("carddemo.card-xref-file", () -> DIRECTORY.resolve("cardxref.txt"));
        registry.add("carddemo.account-file", () -> DIRECTORY.resolve("acctdata.txt"));
        registry.add("carddemo.tran-cat-balance-file", () -> DIRECTORY.resolve("tcatbal.txt"));
        registry.add("carddemo.transaction-file", () -> DIRECTORY.resolve("transact.txt"));
        registry.add("carddemo.daily-reject-file", () -> DIRECTORY.resolve("dalyrejs.txt"));
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job postTranJob;

    @BeforeEach
    void writeFixtures() {
        BatchFixtureFiles.writePostTranData(DIRECTORY);
    }

    @Test
    void postsValidTransactionsAndRejectsTheRestWithReasonCodes() throws Exception {
        jobLauncherTestUtils.setJob(postTranJob);
        JobParameters parameters = new JobParametersBuilder().addLong("run", System.nanoTime()).toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(parameters);

        assertThat(execution.getExitStatus().getExitCode()).isEqualTo(PostTranTasklet.EXIT_CODE_REJECTS);

        List<String> posted = BatchFixtureFiles.readLines(DIRECTORY.resolve("transact.txt"));
        assertThat(posted).hasSize(2);
        assertThat(posted).allSatisfy(record -> assertThat(record).hasSize(TransactionRecord.LENGTH));
        assertThat(posted.stream().map(record -> TransactionRecord.parse(record).getId().trim()))
                .containsExactly("TRAN-OK-1", "TRAN-OK-2");
        assertThat(TransactionRecord.parse(posted.get(0)).getProcessTimestamp().trim()).isNotEmpty();

        List<String> rejects = BatchFixtureFiles.readLines(DIRECTORY.resolve("dalyrejs.txt"));
        assertThat(rejects).hasSize(4);
        assertThat(rejects).allSatisfy(record -> assertThat(record).hasSize(RejectRecord.LENGTH));
        assertThat(rejects.stream().collect(Collectors.toMap(
                record -> TransactionRecord.parse(record).getId().trim(),
                record -> record.substring(350, 354) + "|" + record.substring(354).trim())))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "TRAN-REJ-100", "0100|INVALID CARD NUMBER FOUND",
                        "TRAN-REJ-101", "0101|ACCOUNT RECORD NOT FOUND",
                        "TRAN-REJ-102", "0102|OVERLIMIT TRANSACTION",
                        "TRAN-REJ-103", "0103|TRANSACTION RECEIVED AFTER ACCT EXPIRATION"));
    }

    @Test
    void updatesAccountBalancesAndCategoryBalances() throws Exception {
        jobLauncherTestUtils.setJob(postTranJob);
        jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addLong("run", System.nanoTime()).toJobParameters());

        Map<Long, Account> accounts = BatchFixtureFiles.readLines(DIRECTORY.resolve("acctdata.txt")).stream()
                .map(Account::parse)
                .collect(Collectors.toMap(Account::getAccountId, Function.identity()));

        Account posted = accounts.get(11L);
        assertThat(posted.getCurrentBalance()).isEqualByComparingTo("130.47");
        assertThat(posted.getCurrentCycleCredit()).isEqualByComparingTo("50.47");
        assertThat(posted.getCurrentCycleDebit()).isEqualByComparingTo("-20.00");

        Account rejectedOnly = accounts.get(12L);
        assertThat(rejectedOnly.getCurrentBalance()).isEqualByComparingTo("0.00");

        Map<String, TranCatBalance> balances = BatchFixtureFiles.readLines(DIRECTORY.resolve("tcatbal.txt")).stream()
                .map(TranCatBalance::parse)
                .collect(Collectors.toMap(TranCatBalance::key, Function.identity()));

        assertThat(balances.get(TranCatBalance.key(11L, "01", 1)).getBalance()).isEqualByComparingTo("60.47");
        assertThat(balances.get(TranCatBalance.key(11L, "02", 2)).getBalance())
                .as("a category balance is created on first use")
                .isEqualByComparingTo("-20.00");
    }
}
