package com.carddemo.batch.statement;

import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.testsupport.BatchFixtureFiles;
import com.carddemo.batch.testsupport.Fixtures;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** CBSTM03A: one plain text and one HTML statement per cross reference record. */
@SpringBatchTest
@SpringBootTest
class StatementJobIntegrationTest {

    private static final Path DIRECTORY = BatchFixtureFiles.createDirectory();

    @DynamicPropertySource
    static void files(DynamicPropertyRegistry registry) {
        registry.add("carddemo.card-xref-file", () -> DIRECTORY.resolve("cardxref.txt"));
        registry.add("carddemo.account-file", () -> DIRECTORY.resolve("acctdata.txt"));
        registry.add("carddemo.customer-file", () -> DIRECTORY.resolve("custdata.txt"));
        registry.add("carddemo.transaction-file", () -> DIRECTORY.resolve("transact.txt"));
        registry.add("carddemo.statement-file", () -> DIRECTORY.resolve("stmtfile.txt"));
        registry.add("carddemo.statement-html-file", () -> DIRECTORY.resolve("stmtfile.html"));
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job statementJob;

    private List<String> statements;

    @BeforeEach
    void runJob() throws Exception {
        BatchFixtureFiles.writeReportingData(DIRECTORY);
        jobLauncherTestUtils.setJob(statementJob);
        jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addLong("run", System.nanoTime())
                .toJobParameters());
        statements = BatchFixtureFiles.readLines(DIRECTORY.resolve("stmtfile.txt"));
    }

    @Test
    void printsOneStatementPerCrossReferenceRecordWithTheCustomerAndAccountDetails() {
        assertThat(statements.stream().filter(line -> line.equals(StatementLayout.START_OF_STATEMENT)))
                .hasSize(2);
        assertThat(statements.stream().filter(line -> line.equals(StatementLayout.END_OF_STATEMENT)))
                .hasSize(2);
        assertThat(statements.get(1).trim()).isEqualTo("Ada Lovelace");
        assertThat(statements).anyMatch(line -> line.startsWith("Account ID         :00000000011"));
        assertThat(statements).anyMatch(line -> line.startsWith("FICO Score         :780"));
    }

    @Test
    void printsBalancesAndTransactionAmountsWithTheCobolTrailingSign() {
        assertThat(statements).anyMatch(line -> line.equals("Current Balance    :000000194.00 "
                + " ".repeat(47)));
        assertThat(statements).as("the second account is overpaid, so its balance carries a trailing minus")
                .anyMatch(line -> line.startsWith("Current Balance    :000000050.00-"));
        assertThat(statements.stream().filter(line -> line.startsWith("Total EXP:")).map(String::trim))
                .containsExactly("Total EXP:" + " ".repeat(56) + "$       75.00",
                        "Total EXP:" + " ".repeat(56) + "$     1009.00");
    }

    @Test
    void escapesMasterFileTextInTheHtmlStatement() throws Exception {
        TransactionRecord injected = Fixtures.postedTransaction("TRAN-X-1", BatchFixtureFiles.CARD_A, "01",
                1, "1.00", "2022-06-15 10:00:00.000000");
        injected.setDescription("<script>alert(1)</script>");
        Files.writeString(DIRECTORY.resolve("transact.txt"), injected.format() + System.lineSeparator(),
                StandardCharsets.ISO_8859_1);
        jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addLong("run", System.nanoTime())
                .toJobParameters());

        List<String> html = BatchFixtureFiles.readLines(DIRECTORY.resolve("stmtfile.html"));

        assertThat(html).noneMatch(line -> line.contains("<script>"));
        assertThat(html).anyMatch(line -> line.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    @Test
    void writesAMatchingHtmlStatement() {
        List<String> html = BatchFixtureFiles.readLines(DIRECTORY.resolve("stmtfile.html"));

        assertThat(html.stream().filter(line -> line.equals("<!DOCTYPE html>"))).hasSize(2);
        assertThat(html).contains("<h3>Statement for Account Number: 00000000011</h3>");
        assertThat(html).contains("<p style=\"font-size:16px\">Ada Lovelace</p>");
        assertThat(html).contains("<p>Total EXP: 75.00</p>");
        assertThat(html.get(html.size() - 1)).isEqualTo("</html>");
    }
}
