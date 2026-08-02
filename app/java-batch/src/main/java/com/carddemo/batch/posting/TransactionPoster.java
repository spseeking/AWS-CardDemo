package com.carddemo.batch.posting;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.TranCatBalance;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.store.KeyedRecordStore;

import java.math.BigDecimal;

/** CBTRN02C 2000-POST-TRANSACTION, 2700-UPDATE-TCATBAL and 2800-UPDATE-ACCOUNT-REC. */
public class TransactionPoster {

    /**
     * Applies a validated daily transaction and returns the record to append to the transaction
     * master.
     */
    public TransactionRecord post(TransactionRecord daily, ValidationResult validation,
                                  KeyedRecordStore<TranCatBalance> categoryBalances,
                                  KeyedRecordStore<Account> accounts,
                                  String processTimestamp) {
        long accountId = validation.xref().accountId();

        String key = TranCatBalance.key(accountId, daily.getTypeCode(), daily.getCategoryCode());
        TranCatBalance balance = categoryBalances.find(key)
                .orElseGet(() -> new TranCatBalance(accountId, daily.getTypeCode(), daily.getCategoryCode(),
                        BigDecimal.ZERO.setScale(2)));
        balance.setBalance(balance.getBalance().add(daily.getAmount()));
        categoryBalances.put(balance);

        Account account = validation.account();
        account.setCurrentBalance(account.getCurrentBalance().add(daily.getAmount()));
        if (daily.getAmount().signum() >= 0) {
            account.setCurrentCycleCredit(account.getCurrentCycleCredit().add(daily.getAmount()));
        } else {
            account.setCurrentCycleDebit(account.getCurrentCycleDebit().add(daily.getAmount()));
        }
        accounts.put(account);

        TransactionRecord posted = TransactionRecord.parse(daily.format());
        posted.setProcessTimestamp(processTimestamp);
        return posted;
    }
}
