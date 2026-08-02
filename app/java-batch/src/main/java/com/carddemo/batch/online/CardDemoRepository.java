package com.carddemo.batch.online;

import com.carddemo.batch.config.BatchFilesProperties;
import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.Card;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.Customer;
import com.carddemo.batch.domain.SecurityUser;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.store.KeyedRecordStore;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * The VSAM files the CICS programs read and rewrite. Every call reloads the flat file so the
 * converted transactions see the same committed state a CICS READ would.
 */
@Component
public class CardDemoRepository {

    private final BatchFilesProperties files;

    public CardDemoRepository(BatchFilesProperties files) {
        this.files = files;
    }

    /** USRSEC, keyed on SEC-USR-ID. */
    public KeyedRecordStore<SecurityUser> users() {
        return store(files.getUserSecurityFile(), SecurityUser::parse, user -> user.userId().trim(),
                SecurityUser::format);
    }

    /** ACCTDAT, keyed on ACCT-ID. */
    public KeyedRecordStore<Account> accounts() {
        return store(files.getAccountFile(), Account::parse, account -> String.valueOf(account.getAccountId()),
                Account::format);
    }

    /** CUSTDAT, keyed on CUST-ID. */
    public KeyedRecordStore<Customer> customers() {
        return store(files.getCustomerFile(), Customer::parse,
                customer -> String.valueOf(customer.customerId()), Customer::format);
    }

    /** CARDDAT, keyed on CARD-NUM. */
    public KeyedRecordStore<Card> cards() {
        return store(files.getCardFile(), Card::parse, Card::cardNumber, Card::format);
    }

    /** CCXREF, keyed on XREF-CARD-NUM. The CXACAIX path is served by scanning the same records. */
    public KeyedRecordStore<CardXref> cardXrefs() {
        return store(files.getCardXrefFile(), CardXref::parse, CardXref::cardNumber, CardXref::format);
    }

    /** TRANSACT, keyed on TRAN-ID. */
    public KeyedRecordStore<TransactionRecord> transactions() {
        return store(files.getTransactionFile(), TransactionRecord::parse, TransactionRecord::getId,
                TransactionRecord::format);
    }

    private static <T> KeyedRecordStore<T> store(Path path, java.util.function.Function<String, T> parser,
                                                 java.util.function.Function<T, String> key,
                                                 java.util.function.Function<T, String> formatter) {
        return new KeyedRecordStore<>(path, parser, key, formatter);
    }
}
