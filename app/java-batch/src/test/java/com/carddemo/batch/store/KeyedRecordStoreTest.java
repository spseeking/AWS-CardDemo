package com.carddemo.batch.store;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.testsupport.BatchFixtureFiles;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A rewritten dataset has to come back in key sequence, the way a VSAM KSDS does. */
class KeyedRecordStoreTest {

    private static Account account(long accountId) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setActiveStatus("Y");
        account.setCurrentBalance(BigDecimal.ZERO.setScale(2));
        account.setCreditLimit(BigDecimal.ZERO.setScale(2));
        account.setCashCreditLimit(BigDecimal.ZERO.setScale(2));
        account.setOpenDate("2020-01-01");
        account.setExpirationDate("2030-01-01");
        account.setReissueDate("2020-01-01");
        account.setCurrentCycleCredit(BigDecimal.ZERO.setScale(2));
        account.setCurrentCycleDebit(BigDecimal.ZERO.setScale(2));
        account.setAddressZip("99999");
        account.setGroupId("DEFAULT");
        account.setFiller("");
        return account;
    }

    @Test
    void writesNewRecordsInKeySequenceRatherThanInsertionOrder() throws Exception {
        Path path = BatchFixtureFiles.createDirectory().resolve("acctdata.txt");
        Files.writeString(path, account(30).format() + System.lineSeparator()
                + account(10).format() + System.lineSeparator(), StandardCharsets.ISO_8859_1);

        KeyedRecordStore<Account> store = new KeyedRecordStore<>(path, Account::parse,
                loaded -> String.valueOf(loaded.getAccountId()), Account::format);
        store.put(account(20));
        store.save();

        List<Long> written = Files.readAllLines(path, StandardCharsets.ISO_8859_1).stream()
                .filter(line -> !line.isBlank())
                .map(Account::parse)
                .map(Account::getAccountId)
                .toList();

        assertThat(written).containsExactly(10L, 20L, 30L);
    }
}
