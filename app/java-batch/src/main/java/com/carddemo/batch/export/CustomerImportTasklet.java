package com.carddemo.batch.export;

import com.carddemo.batch.config.BatchFilesProperties;
import com.carddemo.batch.store.SequentialRecordWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CBIMPORT: reads the branch migration file back into the master files and counts the records it
 * could not classify, mirroring the unknown record type error report of the COBOL program.
 */
public class CustomerImportTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(CustomerImportTasklet.class);

    private final BatchFilesProperties files;

    public CustomerImportTasklet(BatchFilesProperties files) {
        this.files = files;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Path directory = files.getImportDirectory();
        int imported = 0;
        int unknown = 0;
        try {
            Files.createDirectories(directory);
            try (InputStream in = Files.newInputStream(files.getExportFile());
                 SequentialRecordWriter customers = writer(directory.resolve("custdata.txt"));
                 SequentialRecordWriter accounts = writer(directory.resolve("acctdata.txt"));
                 SequentialRecordWriter xrefs = writer(directory.resolve("cardxref.txt"));
                 SequentialRecordWriter transactions = writer(directory.resolve("transact.txt"));
                 SequentialRecordWriter cards = writer(directory.resolve("carddata.txt"));
                 SequentialRecordWriter errors = writer(directory.resolve("imperror.txt"))) {
                byte[] record = new byte[ExportRecord.LENGTH];
                while (in.readNBytes(record, 0, ExportRecord.LENGTH) == ExportRecord.LENGTH) {
                    switch (ExportRecord.recordType(record)) {
                        case ExportRecord.TYPE_CUSTOMER ->
                                customers.write(ExportRecord.readCustomer(record).format());
                        case ExportRecord.TYPE_ACCOUNT ->
                                accounts.write(ExportRecord.readAccount(record).format());
                        case ExportRecord.TYPE_XREF -> xrefs.write(ExportRecord.readXref(record).format());
                        case ExportRecord.TYPE_TRANSACTION ->
                                transactions.write(ExportRecord.readTransaction(record).format());
                        case ExportRecord.TYPE_CARD -> cards.write(ExportRecord.readCard(record).format());
                        default -> {
                            unknown++;
                            errors.write("UNKNOWN RECORD TYPE '" + ExportRecord.recordType(record)
                                    + "' SEQUENCE " + ExportRecord.sequenceNumber(record));
                            continue;
                        }
                    }
                    imported++;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to import " + files.getExportFile(), e);
        }

        log.info("CBIMPORT: RECORDS IMPORTED :{}", imported);
        log.info("CBIMPORT: UNKNOWN RECORD TYPES :{}", unknown);
        chunkContext.getStepContext().getStepExecution().getExecutionContext().putInt("imported", imported);
        chunkContext.getStepContext().getStepExecution().getExecutionContext().putInt("unknown", unknown);
        return RepeatStatus.FINISHED;
    }

    private static SequentialRecordWriter writer(Path path) {
        return new SequentialRecordWriter(path);
    }
}
