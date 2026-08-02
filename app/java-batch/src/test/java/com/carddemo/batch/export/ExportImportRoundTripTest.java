package com.carddemo.batch.export;

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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CBEXPORT then CBIMPORT: the packed and binary branch migration records have to come back as the
 * master records they were built from.
 */
@SpringBatchTest
@SpringBootTest
class ExportImportRoundTripTest {

    private static final Path DIRECTORY = BatchFixtureFiles.createDirectory();

    @DynamicPropertySource
    static void files(DynamicPropertyRegistry registry) {
        registry.add("carddemo.card-xref-file", () -> DIRECTORY.resolve("cardxref.txt"));
        registry.add("carddemo.account-file", () -> DIRECTORY.resolve("acctdata.txt"));
        registry.add("carddemo.customer-file", () -> DIRECTORY.resolve("custdata.txt"));
        registry.add("carddemo.card-file", () -> DIRECTORY.resolve("carddata.txt"));
        registry.add("carddemo.transaction-file", () -> DIRECTORY.resolve("transact.txt"));
        registry.add("carddemo.export-file", () -> DIRECTORY.resolve("expfile.dat"));
        registry.add("carddemo.import-directory", () -> DIRECTORY.resolve("import"));
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job customerExportJob;

    @Autowired
    private Job customerImportJob;

    @BeforeEach
    void runBothJobs() throws Exception {
        BatchFixtureFiles.writeReportingData(DIRECTORY);
        run(customerExportJob);
        run(customerImportJob);
    }

    @Test
    void writesFixedLengthRecordsForEveryMasterFileRecord() {
        assertThat(size(DIRECTORY.resolve("expfile.dat")))
                .as("2 customers + 2 accounts + 2 xrefs + 4 transactions + 2 cards")
                .isEqualTo(12L * ExportRecord.LENGTH);
    }

    @Test
    void reconstructsEveryMasterFileByteForByte() {
        assertSameContent("custdata.txt");
        assertSameContent("acctdata.txt");
        assertSameContent("cardxref.txt");
        assertSameContent("transact.txt");
        assertSameContent("carddata.txt");
    }

    @Test
    void reportsNoUnknownRecordTypes() {
        assertThat(BatchFixtureFiles.readLines(DIRECTORY.resolve("import").resolve("imperror.txt"))).isEmpty();
    }

    private void assertSameContent(String fileName) {
        assertThat(BatchFixtureFiles.readLines(DIRECTORY.resolve("import").resolve(fileName)))
                .as(fileName)
                .isEqualTo(BatchFixtureFiles.readLines(DIRECTORY.resolve(fileName)));
    }

    private void run(Job job) throws Exception {
        jobLauncherTestUtils.setJob(job);
        jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addLong("run", System.nanoTime())
                .toJobParameters());
    }

    private static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
