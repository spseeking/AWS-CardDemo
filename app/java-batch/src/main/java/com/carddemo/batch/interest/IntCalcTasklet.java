package com.carddemo.batch.interest;

import com.carddemo.batch.config.BatchFilesProperties;
import com.carddemo.batch.copybook.FixedWidth;
import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.DisclosureGroup;
import com.carddemo.batch.domain.TranCatBalance;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.store.KeyedRecordStore;
import com.carddemo.batch.store.SequentialRecordWriter;
import com.carddemo.batch.support.Db2Timestamp;
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
import java.util.List;
import java.util.Optional;

/**
 * INTCALC / CBACT04C: walks the transaction category balances in key order, accrues monthly
 * interest per category and posts the accrued total to the account when the account changes.
 */
public class IntCalcTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(IntCalcTasklet.class);

    private final BatchFilesProperties files;
    private final InterestCalculator calculator;
    private final String parameterDate;

    public IntCalcTasklet(BatchFilesProperties files, InterestCalculator calculator, String parameterDate) {
        this.files = files;
        this.calculator = calculator;
        this.parameterDate = parameterDate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        KeyedRecordStore<Account> accounts = new KeyedRecordStore<>(files.getAccountFile(),
                Account::parse, account -> Long.toString(account.getAccountId()), Account::format);
        KeyedRecordStore<CardXref> xrefsByAccount = new KeyedRecordStore<>(files.getCardXrefFile(),
                CardXref::parse, xref -> Long.toString(xref.accountId()), CardXref::format);
        KeyedRecordStore<DisclosureGroup> disclosureGroups = new KeyedRecordStore<>(files.getDisclosureGroupFile(),
                DisclosureGroup::parse,
                group -> DisclosureGroupKey.of(group.accountGroupId(), group.transactionTypeCode(),
                        group.transactionCategoryCode()),
                DisclosureGroup::format);

        List<TranCatBalance> categoryBalances = readCategoryBalances();

        Account currentAccount = null;
        CardXref currentXref = null;
        BigDecimal accruedInterest = BigDecimal.ZERO.setScale(2);
        int transactionSuffix = 0;
        int records = 0;

        try (SequentialRecordWriter transactions = new SequentialRecordWriter(files.getTransactionFile())) {
            for (TranCatBalance categoryBalance : categoryBalances) {
                records++;
                if (currentAccount == null || currentAccount.getAccountId() != categoryBalance.getAccountId()) {
                    if (currentAccount != null) {
                        applyInterest(currentAccount, accruedInterest, accounts);
                    }
                    accruedInterest = BigDecimal.ZERO.setScale(2);
                    currentAccount = accounts.find(Long.toString(categoryBalance.getAccountId())).orElse(null);
                    currentXref = xrefsByAccount.find(Long.toString(categoryBalance.getAccountId())).orElse(null);
                    if (currentAccount == null) {
                        log.warn("ACCOUNT NOT FOUND: {}", categoryBalance.getAccountId());
                        continue;
                    }
                }

                Optional<DisclosureGroup> group = calculator.findRate(currentAccount.getGroupId().trim(),
                        categoryBalance.getTransactionTypeCode(), categoryBalance.getTransactionCategoryCode(),
                        disclosureGroups::find);
                BigDecimal rate = group.map(DisclosureGroup::interestRate).orElse(BigDecimal.ZERO);
                if (rate.signum() == 0) {
                    continue;
                }

                BigDecimal monthlyInterest = calculator.monthlyInterest(categoryBalance.getBalance(), rate);
                accruedInterest = accruedInterest.add(monthlyInterest);
                transactionSuffix++;
                transactions.write(interestTransaction(currentAccount, currentXref, monthlyInterest,
                        transactionSuffix).format());
            }

            if (currentAccount != null && !files.isLegacySkipFinalAccountUpdate()) {
                applyInterest(currentAccount, accruedInterest, accounts);
            }
        }

        accounts.save();
        contribution.incrementWriteCount(transactionSuffix);
        contribution.getStepExecution().getExecutionContext().putInt("categoryBalancesRead", records);
        contribution.getStepExecution().getExecutionContext().putInt("interestTransactionsWritten", transactionSuffix);
        log.info("CATEGORY BALANCES READ      :{}", records);
        log.info("INTEREST TRANSACTIONS WRITTEN:{}", transactionSuffix);
        return RepeatStatus.FINISHED;
    }

    private List<TranCatBalance> readCategoryBalances() {
        try {
            return Files.readAllLines(files.getTranCatBalanceFile(), StandardCharsets.ISO_8859_1).stream()
                    .map(line -> line.replace("\r", ""))
                    .filter(line -> !line.isBlank())
                    .map(TranCatBalance::parse)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + files.getTranCatBalanceFile(), e);
        }
    }

    private void applyInterest(Account account, BigDecimal accruedInterest, KeyedRecordStore<Account> accounts) {
        account.setCurrentBalance(account.getCurrentBalance().add(accruedInterest));
        account.setCurrentCycleCredit(BigDecimal.ZERO.setScale(2));
        account.setCurrentCycleDebit(BigDecimal.ZERO.setScale(2));
        accounts.put(account);
    }

    private TransactionRecord interestTransaction(Account account, CardXref xref, BigDecimal amount, int suffix) {
        TransactionRecord tran = new TransactionRecord();
        tran.setId(FixedWidth.chars(parameterDate, 10) + FixedWidth.digits(suffix, 6));
        tran.setTypeCode("01");
        tran.setCategoryCode(5);
        tran.setSource("System");
        tran.setDescription("Int. for a/c " + FixedWidth.digits(account.getAccountId(), 11));
        tran.setAmount(amount);
        tran.setMerchantId(0);
        tran.setCardNumber(xref == null ? "" : xref.cardNumber());
        String timestamp = Db2Timestamp.now();
        tran.setOriginTimestamp(timestamp);
        tran.setProcessTimestamp(timestamp);
        return tran;
    }
}
