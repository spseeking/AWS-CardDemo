package com.carddemo.batch.export;

import com.carddemo.batch.config.BatchFilesProperties;
import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.Card;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.Customer;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.support.Db2Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * CBEXPORT: writes customers, accounts, cross references, transactions and cards to the branch
 * migration file as sequence numbered 500 byte records.
 */
public class CustomerExportTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(CustomerExportTasklet.class);

    private final BatchFilesProperties files;
    private final String branchId;
    private final String regionCode;

    private long sequenceNumber;

    public CustomerExportTasklet(BatchFilesProperties files, String branchId, String regionCode) {
        this.files = files;
        this.branchId = branchId;
        this.regionCode = regionCode;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        sequenceNumber = 0;
        String timestamp = Db2Timestamp.now();
        Path target = files.getExportFile();
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            try (OutputStream out = Files.newOutputStream(target)) {
                export(out, timestamp, ExportRecord.TYPE_CUSTOMER, files.getCustomerFile(), Customer::parse,
                        ExportRecord::writeCustomer);
                export(out, timestamp, ExportRecord.TYPE_ACCOUNT, files.getAccountFile(), Account::parse,
                        ExportRecord::writeAccount);
                export(out, timestamp, ExportRecord.TYPE_XREF, files.getCardXrefFile(), CardXref::parse,
                        ExportRecord::writeXref);
                export(out, timestamp, ExportRecord.TYPE_TRANSACTION, files.getTransactionFile(),
                        TransactionRecord::parse, ExportRecord::writeTransaction);
                export(out, timestamp, ExportRecord.TYPE_CARD, files.getCardFile(), Card::parse,
                        ExportRecord::writeCard);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write " + target, e);
        }

        log.info("CBEXPORT: RECORDS EXPORTED :{}", sequenceNumber);
        chunkContext.getStepContext().getStepExecution().getExecutionContext()
                .putLong("exported", sequenceNumber);
        return RepeatStatus.FINISHED;
    }

    private <T> void export(OutputStream out, String timestamp, char recordType, Path source,
                            Function<String, T> parser, BiConsumer<byte[], T> writer) throws IOException {
        for (String line : readLines(source)) {
            T value = parser.apply(line);
            byte[] record = ExportRecord.header(recordType, timestamp, ++sequenceNumber, branchId, regionCode);
            writer.accept(record, value);
            out.write(record);
        }
    }

    private List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.ISO_8859_1).stream()
                    .map(line -> line.replace("\r", ""))
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + path, e);
        }
    }
}
