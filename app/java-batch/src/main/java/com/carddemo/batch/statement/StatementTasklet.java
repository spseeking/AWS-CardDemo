package com.carddemo.batch.statement;

import com.carddemo.batch.config.BatchFilesProperties;
import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.Customer;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CBSTM03A (JCL CREASTMT): prints a plain text and an HTML statement per cross reference record.
 * The mainframe program reaches its files through the CBSTM03B I/O subprogram and a TIOT walk;
 * both are replaced here by direct file access.
 */
public class StatementTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(StatementTasklet.class);

    private final BatchFilesProperties files;

    public StatementTasklet(BatchFilesProperties files) {
        this.files = files;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        KeyedRecordStore<Customer> customers = new KeyedRecordStore<>(files.getCustomerFile(), Customer::parse,
                customer -> String.valueOf(customer.customerId()), Customer::format);
        KeyedRecordStore<Account> accounts = new KeyedRecordStore<>(files.getAccountFile(), Account::parse,
                account -> String.valueOf(account.getAccountId()), Account::format);
        Map<String, List<TransactionRecord>> transactionsByCard = transactionsByCard();

        int statements = 0;
        try (SequentialRecordWriter text = new SequentialRecordWriter(files.getStatementFile());
             SequentialRecordWriter html = new SequentialRecordWriter(files.getStatementHtmlFile())) {
            for (String line : readLines(files.getCardXrefFile())) {
                CardXref xref = CardXref.parse(line);
                Customer customer = customers.find(String.valueOf(xref.customerId())).orElse(null);
                Account account = accounts.find(String.valueOf(xref.accountId())).orElse(null);
                if (customer == null || account == null) {
                    log.warn("Skipping statement for card {}: customer or account missing", xref.cardNumber());
                    continue;
                }
                List<TransactionRecord> transactions =
                        transactionsByCard.getOrDefault(xref.cardNumber(), List.of());
                writeText(text, customer, account, transactions);
                new StatementHtmlWriter(html).write(customer, account, transactions);
                statements++;
            }
        }

        log.info("STATEMENTS WRITTEN :{}", statements);
        chunkContext.getStepContext().getStepExecution().getExecutionContext().putInt("statements", statements);
        return RepeatStatus.FINISHED;
    }

    private void writeText(SequentialRecordWriter out, Customer customer, Account account,
                           List<TransactionRecord> transactions) {
        out.write(StatementLayout.START_OF_STATEMENT);
        out.write(StatementLayout.name(customer.fullName()));
        out.write(StatementLayout.addressLine(customer.addressLine1()));
        out.write(StatementLayout.addressLine(customer.addressLine2()));
        out.write(StatementLayout.addressLine3(customer.addressLine3().trim() + " "
                + customer.stateCode().trim() + " " + customer.countryCode().trim() + " "
                + customer.zip().trim()));
        out.write(StatementLayout.RULE);
        out.write(StatementLayout.centered("Basic Details", 33, 14, 33));
        out.write(StatementLayout.RULE);
        out.write(StatementLayout.accountId(account.getAccountId()));
        out.write(StatementLayout.currentBalance(account.getCurrentBalance()));
        out.write(StatementLayout.ficoScore(customer.ficoScore()));
        out.write(StatementLayout.RULE);
        out.write(StatementLayout.centered("TRANSACTION SUMMARY ", 30, 20, 30));
        out.write(StatementLayout.RULE);
        out.write(StatementLayout.transactionHeader());

        BigDecimal total = BigDecimal.ZERO;
        for (TransactionRecord transaction : transactions) {
            out.write(StatementLayout.transaction(transaction.getId(), transaction.getDescription(),
                    transaction.getAmount()));
            total = total.add(transaction.getAmount());
        }
        out.write(StatementLayout.RULE);
        out.write(StatementLayout.total(total));
        out.write(StatementLayout.END_OF_STATEMENT);
    }

    private Map<String, List<TransactionRecord>> transactionsByCard() {
        Map<String, List<TransactionRecord>> byCard = new LinkedHashMap<>();
        for (String line : readLines(files.getTransactionFile())) {
            TransactionRecord transaction = TransactionRecord.parse(line);
            byCard.computeIfAbsent(transaction.getCardNumber(), key -> new ArrayList<>()).add(transaction);
        }
        return byCard;
    }

    private List<String> readLines(java.nio.file.Path path) {
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
