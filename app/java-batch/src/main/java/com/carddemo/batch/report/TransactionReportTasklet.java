package com.carddemo.batch.report;

import com.carddemo.batch.config.BatchFilesProperties;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.TranCategory;
import com.carddemo.batch.domain.TranType;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.store.KeyedRecordStore;
import com.carddemo.batch.store.SequentialRecordWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** CBTRN03C (JCL TRANREPT): prints the daily transaction detail report with running totals. */
public class TransactionReportTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(TransactionReportTasklet.class);
    private static final int PAGE_SIZE = 20;

    private final BatchFilesProperties files;

    private BigDecimal pageTotal = BigDecimal.ZERO;
    private BigDecimal accountTotal = BigDecimal.ZERO;
    private BigDecimal grandTotal = BigDecimal.ZERO;
    private long lineCounter;
    private boolean firstTime = true;
    private String currentCardNumber = "";
    private long currentAccountId;

    public TransactionReportTasklet(BatchFilesProperties files) {
        this.files = files;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        reset();
        DateRange range = readDateRange();
        KeyedRecordStore<CardXref> xrefs = new KeyedRecordStore<>(files.getCardXrefFile(), CardXref::parse,
                CardXref::cardNumber, CardXref::format);
        KeyedRecordStore<TranType> types = new KeyedRecordStore<>(files.getTranTypeFile(), TranType::parse,
                TranType::typeCode, TranType::format);
        KeyedRecordStore<TranCategory> categories = new KeyedRecordStore<>(files.getTranCategoryFile(),
                TranCategory::parse, TranCategory::key, TranCategory::format);

        int reported = 0;
        BigDecimal lastAmount = BigDecimal.ZERO;
        try (SequentialRecordWriter report = new SequentialRecordWriter(files.getTransactionReportFile())) {
            for (String line : readLines(files.getTransactionFile())) {
                TransactionRecord transaction = TransactionRecord.parse(line);
                String processedOn = transaction.getProcessTimestamp();
                processedOn = processedOn.length() >= 10 ? processedOn.substring(0, 10) : processedOn;
                if (processedOn.compareTo(range.start()) < 0 || processedOn.compareTo(range.end()) > 0) {
                    if (files.isLegacyStopReportAtOutOfRangeRecord()) {
                        break;
                    }
                    continue;
                }

                if (!currentCardNumber.equals(transaction.getCardNumber())) {
                    if (!firstTime) {
                        writeAccountTotals(report);
                    }
                    currentCardNumber = transaction.getCardNumber();
                    currentAccountId = xrefs.find(currentCardNumber).map(CardXref::accountId).orElse(0L);
                }

                String typeDescription = types.find(transaction.getTypeCode())
                        .map(TranType::description).orElse("");
                String categoryDescription = categories
                        .find(TranCategory.key(transaction.getTypeCode(), transaction.getCategoryCode()))
                        .map(TranCategory::description).orElse("");

                if (firstTime) {
                    firstTime = false;
                    writeHeaders(report, range);
                } else if (lineCounter % PAGE_SIZE == 0) {
                    writePageTotals(report);
                    writeHeaders(report, range);
                }

                lastAmount = transaction.getAmount();
                pageTotal = pageTotal.add(lastAmount);
                accountTotal = accountTotal.add(lastAmount);
                report.write(TransactionReportLayout.detail(transaction.getId(), currentAccountId,
                        transaction.getTypeCode(), typeDescription, transaction.getCategoryCode(),
                        categoryDescription, transaction.getSource(), lastAmount));
                lineCounter++;
                reported++;
            }

            if (files.isLegacyReportDoubleCountsLastAmount()) {
                // CBTRN03C adds TRAN-AMT once more from the record area at end of file.
                pageTotal = pageTotal.add(lastAmount);
                accountTotal = accountTotal.add(lastAmount);
            }
            writePageTotals(report);
            report.write(TransactionReportLayout.grandTotal(grandTotal));
        }

        log.info("TRANSACTIONS REPORTED :{}", reported);
        chunkContext.getStepContext().getStepExecution().getExecutionContext().putInt("reported", reported);
        contribution.incrementReadCount();
        return RepeatStatus.FINISHED;
    }

    private void reset() {
        pageTotal = BigDecimal.ZERO;
        accountTotal = BigDecimal.ZERO;
        grandTotal = BigDecimal.ZERO;
        lineCounter = 0;
        firstTime = true;
        currentCardNumber = "";
        currentAccountId = 0;
    }

    private void writeHeaders(SequentialRecordWriter report, DateRange range) {
        report.write(TransactionReportLayout.nameHeader(range.start(), range.end()));
        report.write(TransactionReportLayout.BLANK);
        report.write(TransactionReportLayout.COLUMN_HEADER);
        report.write(TransactionReportLayout.SEPARATOR);
        lineCounter += 4;
    }

    private void writePageTotals(SequentialRecordWriter report) {
        report.write(TransactionReportLayout.pageTotal(pageTotal));
        grandTotal = grandTotal.add(pageTotal);
        pageTotal = BigDecimal.ZERO;
        lineCounter++;
        report.write(TransactionReportLayout.SEPARATOR);
        lineCounter++;
    }

    private void writeAccountTotals(SequentialRecordWriter report) {
        report.write(TransactionReportLayout.accountTotal(accountTotal));
        accountTotal = BigDecimal.ZERO;
        lineCounter++;
        report.write(TransactionReportLayout.SEPARATOR);
        lineCounter++;
    }

    private DateRange readDateRange() {
        List<String> lines = readLines(files.getDateParmFile());
        if (lines.isEmpty()) {
            throw new IllegalStateException("DATEPARM file " + files.getDateParmFile() + " is empty");
        }
        String record = lines.get(0);
        DateRange range = new DateRange(record.substring(0, 10), record.substring(11, 21));
        log.info("Reporting from {} to {}", range.start(), range.end());
        return range;
    }

    private static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.ISO_8859_1).stream()
                    .map(line -> line.replace("\r", ""))
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + path, e);
        }
    }

    private record DateRange(String start, String end) {
    }
}
