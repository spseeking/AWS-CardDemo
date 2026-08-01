package com.carddemo.batch.posting;

import com.carddemo.batch.config.BatchFilesProperties;
import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.TranCatBalance;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.store.KeyedRecordStore;
import com.carddemo.batch.store.SequentialRecordWriter;
import com.carddemo.batch.support.Db2Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;

/**
 * POSTTRAN / CBTRN02C: reads the daily transaction file, validates each record and either posts
 * it or writes it to the rejects file with a reason code.
 *
 * <p>The COBOL program sets RETURN-CODE 4 when anything was rejected; the step mirrors that with
 * the {@code COMPLETED WITH REJECTS} exit status.
 */
public class PostTranTasklet implements Tasklet {

    public static final String EXIT_CODE_REJECTS = "COMPLETED WITH REJECTS";

    private static final Logger log = LoggerFactory.getLogger(PostTranTasklet.class);

    private final BatchFilesProperties files;
    private final TransactionValidator validator;
    private final TransactionPoster poster;

    public PostTranTasklet(BatchFilesProperties files, TransactionValidator validator, TransactionPoster poster) {
        this.files = files;
        this.validator = validator;
        this.poster = poster;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        KeyedRecordStore<CardXref> xrefs = new KeyedRecordStore<>(files.getCardXrefFile(),
                CardXref::parse, CardXref::cardNumber, CardXref::format);
        KeyedRecordStore<Account> accounts = new KeyedRecordStore<>(files.getAccountFile(),
                Account::parse, account -> Long.toString(account.getAccountId()), Account::format);
        KeyedRecordStore<TranCatBalance> categoryBalances = new KeyedRecordStore<>(files.getTranCatBalanceFile(),
                TranCatBalance::parse, TranCatBalance::key, TranCatBalance::format);

        int processed = 0;
        int rejected = 0;

        try (SequentialRecordWriter transactions = new SequentialRecordWriter(files.getTransactionFile());
             SequentialRecordWriter rejects = new SequentialRecordWriter(files.getDailyRejectFile());
             Stream<String> lines = Files.lines(files.getDailyTransactionFile(), StandardCharsets.ISO_8859_1)) {

            String processTimestamp = Db2Timestamp.now();
            for (String line : (Iterable<String>) lines.map(l -> l.replace("\r", ""))
                    .filter(l -> !l.isBlank())::iterator) {
                processed++;
                TransactionRecord daily = TransactionRecord.parse(line);
                ValidationResult result = validator.validate(daily, xrefs::find,
                        accountId -> accounts.find(Long.toString(accountId)));

                if (result.isAccepted()) {
                    transactions.write(poster.post(daily, result, categoryBalances, accounts, processTimestamp)
                            .format());
                } else {
                    rejected++;
                    rejects.write(RejectRecord.format(daily.format(), result.reasonCode(),
                            result.reasonDescription()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + files.getDailyTransactionFile(), e);
        }

        accounts.save();
        categoryBalances.save();

        contribution.incrementReadCount();
        contribution.incrementWriteCount(processed - rejected);
        contribution.getStepExecution().getExecutionContext().putInt("transactionsProcessed", processed);
        contribution.getStepExecution().getExecutionContext().putInt("transactionsRejected", rejected);
        log.info("TRANSACTIONS PROCESSED :{}", processed);
        log.info("TRANSACTIONS REJECTED  :{}", rejected);
        if (rejected > 0) {
            contribution.setExitStatus(new ExitStatus(EXIT_CODE_REJECTS));
        }
        return RepeatStatus.FINISHED;
    }
}
