package com.carddemo.batch.online;

import com.carddemo.batch.copybook.FixedWidth;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.store.KeyedRecordStore;
import com.carddemo.batch.support.Db2Timestamp;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** COTRN00C (list), COTRN01C (view) and COTRN02C (add) against the TRANSACT file. */
@Service
public class TransactionService {

    /** The COTRN02C input map, keyed by either account id or card number. */
    public record NewTransaction(String accountId, String cardNumber, String typeCode, String categoryCode,
                                 String source, String description, String amount, String originTimestamp,
                                 String processTimestamp, String merchantId, String merchantName,
                                 String merchantCity, String merchantZip) {
    }

    private static final int TRAN_ID_LENGTH = 16;
    private static final String AMOUNT_PATTERN = "-?\\d{1,8}\\.\\d{2}";
    private static final String DATE_PATTERN = "\\d{4}-\\d{2}-\\d{2}";

    private final CardDemoRepository repository;

    public TransactionService(CardDemoRepository repository) {
        this.repository = repository;
    }

    public List<TransactionRecord> list() {
        List<TransactionRecord> transactions = new ArrayList<>();
        repository.transactions().values().forEach(transactions::add);
        return transactions;
    }

    public TransactionRecord view(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new BusinessRuleException("Tran ID can NOT be empty...");
        }
        return repository.transactions().find(FixedWidth.chars(transactionId.trim(), TRAN_ID_LENGTH))
                .orElseThrow(() -> new BusinessRuleException("Transaction ID NOT found..."));
    }

    public TransactionRecord add(NewTransaction input) {
        String cardNumber = resolveCardNumber(input);
        validate(input);

        KeyedRecordStore<TransactionRecord> transactions = repository.transactions();
        TransactionRecord transaction = new TransactionRecord();
        transaction.setId(nextTransactionId(transactions));
        transaction.setTypeCode(input.typeCode());
        transaction.setCategoryCode(Integer.parseInt(input.categoryCode().trim()));
        transaction.setSource(input.source());
        transaction.setDescription(input.description());
        transaction.setAmount(new BigDecimal(input.amount()));
        transaction.setMerchantId(Long.parseLong(input.merchantId().trim()));
        transaction.setMerchantName(input.merchantName());
        transaction.setMerchantCity(input.merchantCity());
        transaction.setMerchantZip(input.merchantZip());
        transaction.setCardNumber(cardNumber);
        transaction.setOriginTimestamp(input.originTimestamp());
        transaction.setProcessTimestamp(input.processTimestamp());
        transaction.setFiller("");

        transactions.put(transaction);
        transactions.save();
        return transaction;
    }

    /** COTRN02C browses TRANSACT backwards from HIGH-VALUES and adds one to the highest key. */
    static String nextTransactionId(KeyedRecordStore<TransactionRecord> transactions) {
        long highest = 0L;
        for (TransactionRecord existing : transactions.values()) {
            String id = existing.getId().trim();
            if (id.matches("\\d+")) {
                highest = Math.max(highest, Long.parseLong(id));
            }
        }
        return FixedWidth.digits(highest + 1, TRAN_ID_LENGTH);
    }

    private String resolveCardNumber(NewTransaction input) {
        if (input.accountId() != null && !input.accountId().isBlank()) {
            if (!input.accountId().trim().matches("\\d+")) {
                throw new BusinessRuleException("Account ID must be Numeric...");
            }
            long accountId = Long.parseLong(input.accountId().trim());
            return xrefByAccount(accountId)
                    .orElseThrow(() -> new BusinessRuleException("Account ID NOT found..."))
                    .cardNumber();
        }
        if (input.cardNumber() != null && !input.cardNumber().isBlank()) {
            if (!input.cardNumber().trim().matches("\\d+")) {
                throw new BusinessRuleException("Card Number must be Numeric...");
            }
            return repository.cardXrefs().find(input.cardNumber().trim())
                    .orElseThrow(() -> new BusinessRuleException("Card Number NOT found..."))
                    .cardNumber();
        }
        throw new BusinessRuleException("Account or Card Number must be entered...");
    }

    private Optional<CardXref> xrefByAccount(long accountId) {
        for (CardXref xref : repository.cardXrefs().values()) {
            if (xref.accountId() == accountId) {
                return Optional.of(xref);
            }
        }
        return Optional.empty();
    }

    private static void validate(NewTransaction input) {
        require(input.typeCode(), "Type CD can NOT be empty...");
        require(input.categoryCode(), "Category CD can NOT be empty...");
        require(input.source(), "Source can NOT be empty...");
        require(input.description(), "Description can NOT be empty...");
        require(input.amount(), "Amount can NOT be empty...");
        require(input.originTimestamp(), "Orig Date can NOT be empty...");
        require(input.processTimestamp(), "Proc Date can NOT be empty...");
        require(input.merchantId(), "Merchant ID can NOT be empty...");
        require(input.merchantName(), "Merchant Name can NOT be empty...");
        require(input.merchantCity(), "Merchant City can NOT be empty...");
        require(input.merchantZip(), "Merchant Zip can NOT be empty...");

        if (!input.amount().trim().matches(AMOUNT_PATTERN)) {
            throw new BusinessRuleException("Amount should be in format -99999999.99");
        }
        if (!datePart(input.originTimestamp()).matches(DATE_PATTERN)) {
            throw new BusinessRuleException("Orig Date should be in format YYYY-MM-DD");
        }
        if (!datePart(input.processTimestamp()).matches(DATE_PATTERN)) {
            throw new BusinessRuleException("Proc Date should be in format YYYY-MM-DD");
        }
        if (!input.merchantId().trim().matches("\\d+")) {
            throw new BusinessRuleException("Merchant ID must be Numeric...");
        }
    }

    private static String datePart(String timestamp) {
        String value = timestamp.trim();
        return value.length() >= 10 ? value.substring(0, 10) : value;
    }

    private static void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(message);
        }
    }

    static String timestamp() {
        return Db2Timestamp.now();
    }
}
