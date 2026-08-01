package com.carddemo.batch.online;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.store.KeyedRecordStore;
import com.carddemo.batch.support.Db2Timestamp;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * COBIL00C: pays the full account balance, writing a type 02 category 2 transaction and zeroing
 * ACCT-CURR-BAL. The screen only pays once the confirmation flag is Y.
 */
@Service
public class BillPaymentService {

    /** What the screen showed after the payment: the posted transaction and the new balance. */
    public record BillPaymentResult(String transactionId, BigDecimal amountPaid, BigDecimal currentBalance) {
    }

    private final CardDemoRepository repository;

    public BillPaymentService(CardDemoRepository repository) {
        this.repository = repository;
    }

    public BigDecimal balance(String accountId) {
        return account(accountId).getCurrentBalance();
    }

    public BillPaymentResult pay(String accountId, String confirmation) {
        Account account = account(accountId);
        if (confirmation == null || confirmation.isBlank()) {
            throw new BusinessRuleException("Confirm to make a bill payment...");
        }
        if (!"Y".equalsIgnoreCase(confirmation) && !"N".equalsIgnoreCase(confirmation)) {
            throw new BusinessRuleException("Invalid value. Valid values are (Y/N)...");
        }
        if ("N".equalsIgnoreCase(confirmation)) {
            throw new BusinessRuleException("Confirm to make a bill payment...");
        }
        if (account.getCurrentBalance().signum() <= 0) {
            throw new BusinessRuleException("You have nothing to pay...");
        }

        CardXref xref = xrefByAccount(account.getAccountId())
                .orElseThrow(() -> new BusinessRuleException("Account ID NOT found..."));
        KeyedRecordStore<TransactionRecord> transactions = repository.transactions();

        BigDecimal amount = account.getCurrentBalance();
        String timestamp = Db2Timestamp.now();
        TransactionRecord payment = new TransactionRecord();
        payment.setId(TransactionService.nextTransactionId(transactions));
        payment.setTypeCode("02");
        payment.setCategoryCode(2);
        payment.setSource("POS TERM");
        payment.setDescription("BILL PAYMENT - ONLINE");
        payment.setAmount(amount);
        payment.setCardNumber(xref.cardNumber());
        payment.setMerchantId(999999999L);
        payment.setMerchantName("BILL PAYMENT");
        payment.setMerchantCity("N/A");
        payment.setMerchantZip("N/A");
        payment.setOriginTimestamp(timestamp);
        payment.setProcessTimestamp(timestamp);
        payment.setFiller("");
        transactions.put(payment);
        transactions.save();

        KeyedRecordStore<Account> accounts = repository.accounts();
        account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
        accounts.put(account);
        accounts.save();

        return new BillPaymentResult(payment.getId(), amount, account.getCurrentBalance());
    }

    private Account account(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new BusinessRuleException("Acct ID can NOT be empty...");
        }
        return repository.accounts().find(String.valueOf(Long.parseLong(accountId.trim())))
                .orElseThrow(() -> new BusinessRuleException("Account ID NOT found..."));
    }

    private Optional<CardXref> xrefByAccount(long accountId) {
        for (CardXref xref : repository.cardXrefs().values()) {
            if (xref.accountId() == accountId) {
                return Optional.of(xref);
            }
        }
        return Optional.empty();
    }
}
