package com.carddemo.batch.report;

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

import static org.assertj.core.api.Assertions.assertThat;

/** CBTRN03C: date range filtering, cross referenced account ids and the running totals. */
@SpringBatchTest
@SpringBootTest
class TransactionReportJobIntegrationTest {

    private static final Path DIRECTORY = BatchFixtureFiles.createDirectory();

    @DynamicPropertySource
    static void files(DynamicPropertyRegistry registry) {
        registry.add("carddemo.card-xref-file", () -> DIRECTORY.resolve("cardxref.txt"));
        registry.add("carddemo.transaction-file", () -> DIRECTORY.resolve("transact.txt"));
        registry.add("carddemo.tran-type-file", () -> DIRECTORY.resolve("trantype.txt"));
        registry.add("carddemo.tran-category-file", () -> DIRECTORY.resolve("trancatg.txt"));
        registry.add("carddemo.date-parm-file", () -> DIRECTORY.resolve("dateparm.txt"));
        registry.add("carddemo.transaction-report-file", () -> DIRECTORY.resolve("tranrept.txt"));
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job transactionReportJob;

    private List<String> report;

    @BeforeEach
    void runJob() throws Exception {
        BatchFixtureFiles.writeReportingData(DIRECTORY);
        jobLauncherTestUtils.setJob(transactionReportJob);
        jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addLong("run", System.nanoTime())
                .toJobParameters());
        report = BatchFixtureFiles.readLines(DIRECTORY.resolve("tranrept.txt"));
    }

    @Test
    void printsHeadersAndOneFixedWidthDetailLinePerInRangeTransaction() {
        assertThat(report).allSatisfy(line ->
                assertThat(line).hasSize(TransactionReportLayout.LINE_LENGTH));
        assertThat(report.get(0)).startsWith("DALYREPT");
        assertThat(report.get(0)).contains("Date Range: 2022-06-01 to 2022-06-30");
        assertThat(report.stream().filter(line -> line.startsWith("TRAN-")).map(String::trim))
                .containsExactly(
                        "TRAN-A-1         00000000011 01-Purchase        0001-Regular Sales Draft"
                                + "           POS TERM               100.00",
                        "TRAN-A-2         00000000011 02-Payment         0002-Payment Received"
                                + "              POS TERM      -         25.00",
                        "TRAN-C-1         00000000012 01-Purchase        0001-Regular Sales Draft"
                                + "           POS TERM                10.00");
    }

    @Test
    void leavesTransactionsOutsideTheDateRangeOutOfTheReport() {
        assertThat(report).noneSatisfy(line -> assertThat(line).contains("TRAN-OLD"));
    }

    @Test
    void closesEachCardWithAnAccountTotalAndTheRunWithPageAndGrandTotals() {
        assertThat(report.stream().filter(line -> line.startsWith("Account Total")).map(String::trim))
                .as("75.00 for the first card, second card is closed by the end of file logic")
                .containsExactly("Account Total" + ".".repeat(84) + "+         75.00");
        assertThat(report.stream().filter(line -> line.startsWith("Page Total")).map(String::trim))
                .as("the last amount is counted twice, reproducing the CBTRN03C end of file branch")
                .containsExactly("Page Total " + ".".repeat(86) + "+         95.00");
        assertThat(report.get(report.size() - 1).trim())
                .isEqualTo("Grand Total" + ".".repeat(86) + "+         95.00");
    }
}
