package com.carddemo.batch.online;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.Customer;
import com.carddemo.batch.store.KeyedRecordStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/** COACTVWC (account view) and COACTUPC (account update) against ACCTDAT, CUSTDAT and CXACAIX. */
@Service
public class AccountService {

    /** The joined account / customer view COACTVWC builds from ACCTDAT, CXACAIX and CUSTDAT. */
    public record AccountView(Account account, Customer customer, String cardNumber) {
    }

    /** The updatable subset of the COACTUPC screen. */
    public record AccountUpdate(String activeStatus, BigDecimal creditLimit, BigDecimal cashCreditLimit,
                                String expirationDate, String reissueDate) {
    }

    private final CardDemoRepository repository;

    public AccountService(CardDemoRepository repository) {
        this.repository = repository;
    }

    public AccountView view(String accountId) {
        long id = validateAccountId(accountId);
        Account account = repository.accounts().find(String.valueOf(id))
                .orElseThrow(() -> new BusinessRuleException("Account not found in ACCTDAT"));
        Optional<CardXref> xref = findXref(id);
        Customer customer = xref.flatMap(found -> repository.customers()
                        .find(String.valueOf(found.customerId())))
                .orElseThrow(() -> new BusinessRuleException("Customer not found in CUSTDAT"));
        return new AccountView(account, customer, xref.map(CardXref::cardNumber).orElse(""));
    }

    public Account update(String accountId, AccountUpdate update) {
        long id = validateAccountId(accountId);
        KeyedRecordStore<Account> accounts = repository.accounts();
        Account account = accounts.find(String.valueOf(id))
                .orElseThrow(() -> new BusinessRuleException("Account not found in ACCTDAT"));

        if (!"Y".equalsIgnoreCase(update.activeStatus()) && !"N".equalsIgnoreCase(update.activeStatus())) {
            throw new BusinessRuleException("Account Active Status must be Y or N");
        }
        if (update.creditLimit() == null) {
            throw new BusinessRuleException("Credit Limit must be supplied");
        }
        if (update.cashCreditLimit() == null) {
            throw new BusinessRuleException("Cash Credit Limit must be supplied");
        }
        validateDate(update.expirationDate(), "Expiry");
        validateDate(update.reissueDate(), "Reissue");

        account.setActiveStatus(update.activeStatus().toUpperCase(java.util.Locale.ROOT));
        account.setCreditLimit(update.creditLimit());
        account.setCashCreditLimit(update.cashCreditLimit());
        account.setExpirationDate(update.expirationDate());
        account.setReissueDate(update.reissueDate());
        accounts.put(account);
        accounts.save();
        return account;
    }

    private Optional<CardXref> findXref(long accountId) {
        for (CardXref xref : repository.cardXrefs().values()) {
            if (xref.accountId() == accountId) {
                return Optional.of(xref);
            }
        }
        return Optional.empty();
    }

    /** COACTUPC only accepts a non zero eleven digit account number. */
    private static long validateAccountId(String accountId) {
        String value = accountId == null ? "" : accountId.trim();
        if (!value.matches("\\d{1,11}") || Long.parseLong(value) == 0L) {
            throw new BusinessRuleException("Account number must be a non zero 11 digit number");
        }
        return Long.parseLong(value);
    }

    private static void validateDate(String value, String label) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BusinessRuleException(label + " date should be in format YYYY-MM-DD");
        }
        int month = Integer.parseInt(value.substring(5, 7));
        if (month < 1 || month > 12) {
            throw new BusinessRuleException("Card expiry month must be between 1 and 12");
        }
    }
}
