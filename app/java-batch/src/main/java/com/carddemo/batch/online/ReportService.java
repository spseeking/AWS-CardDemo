package com.carddemo.batch.online;

import com.carddemo.batch.config.BatchFilesProperties;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CORPT00C: instead of submitting the TRANREPT job through the internal reader, this launches the
 * converted Spring Batch report job with the same monthly, yearly or custom date range.
 */
@Service
public class ReportService {

    /** The three CORPT00C choices. */
    public enum ReportType { MONTHLY, YEARLY, CUSTOM }

    /** The submitted range plus the job execution id that replaced the JCL job number. */
    public record ReportSubmission(String startDate, String endDate, long executionId) {
    }

    /** DATEPARM, the report dataset and the tasklet totals are all shared, as the JCL initiator was. */
    private final ReentrantLock submission = new ReentrantLock();

    private final JobLauncher jobLauncher;
    private final Job transactionReportJob;
    private final BatchFilesProperties files;

    public ReportService(JobLauncher jobLauncher,
                         @Qualifier("transactionReportJob") Job transactionReportJob,
                         BatchFilesProperties files) {
        this.jobLauncher = jobLauncher;
        this.transactionReportJob = transactionReportJob;
        this.files = files;
    }

    public ReportSubmission submit(ReportType type, String startDate, String endDate, String confirmation) {
        if (confirmation == null || !"Y".equalsIgnoreCase(confirmation)) {
            throw new BusinessRuleException("Invalid value. Valid values are (Y/N)...");
        }

        LocalDate today = LocalDate.now();
        String start;
        String end;
        switch (type) {
            case MONTHLY -> {
                start = today.withDayOfMonth(1).toString();
                end = today.with(TemporalAdjusters.lastDayOfMonth()).toString();
            }
            case YEARLY -> {
                start = today.withDayOfYear(1).toString();
                end = today.with(TemporalAdjusters.lastDayOfYear()).toString();
            }
            default -> {
                start = parse(startDate, "Start Date");
                end = parse(endDate, "End Date");
                if (start.compareTo(end) > 0) {
                    throw new BusinessRuleException("Start Date should be before End Date...");
                }
            }
        }

        submission.lock();
        try {
            writeDateParm(start, end);
            long executionId = jobLauncher.run(transactionReportJob, new JobParametersBuilder()
                    .addString("startDate", start)
                    .addString("endDate", end)
                    .addLong("submittedAt", System.currentTimeMillis())
                    .toJobParameters()).getId();
            return new ReportSubmission(start, end, executionId);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to submit the transaction report job", e);
        } finally {
            submission.unlock();
        }
    }

    private void writeDateParm(String start, String end) {
        try {
            if (files.getDateParmFile().getParent() != null) {
                Files.createDirectories(files.getDateParmFile().getParent());
            }
            Files.writeString(files.getDateParmFile(), start + " " + end + System.lineSeparator(),
                    StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write " + files.getDateParmFile(), e);
        }
    }

    private static String parse(String value, String label) {
        try {
            return LocalDate.parse(value == null ? "" : value.trim()).toString();
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException(label + " should be in format YYYY-MM-DD");
        }
    }
}
