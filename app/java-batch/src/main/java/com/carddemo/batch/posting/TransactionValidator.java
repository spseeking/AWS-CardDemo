package com.carddemo.batch.posting;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.TransactionRecord;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;

/** CBTRN02C 1500-VALIDATE-TRAN. */
public class TransactionValidator {

    public ValidationResult validate(TransactionRecord daily,
                                     Function<String, Optional<CardXref>> xrefLookup,
                                     LongFunction<Optional<Account>> accountLookup) {
        Optional<CardXref> xref = xrefLookup.apply(daily.getCardNumber());
        if (xref.isEmpty()) {
            return ValidationResult.rejected(ValidationResult.INVALID_CARD_NUMBER,
                    "INVALID CARD NUMBER FOUND", null, null);
        }

        Optional<Account> account = accountLookup.apply(xref.get().accountId());
        if (account.isEmpty()) {
            return ValidationResult.rejected(ValidationResult.ACCOUNT_NOT_FOUND,
                    "ACCOUNT RECORD NOT FOUND", xref.get(), null);
        }

        Account acct = account.get();
        int reasonCode = ValidationResult.OK;
        String reasonDescription = "";

        BigDecimal projectedBalance = acct.getCurrentCycleCredit()
                .subtract(acct.getCurrentCycleDebit())
                .add(daily.getAmount());
        if (acct.getCreditLimit().compareTo(projectedBalance) < 0) {
            reasonCode = ValidationResult.OVERLIMIT;
            reasonDescription = "OVERLIMIT TRANSACTION";
        }

        // CBTRN02C evaluates the expiry check unconditionally, so an expired account overrides an
        // overlimit reason code when both fail.
        String originDate = daily.getOriginTimestamp().length() >= 10
                ? daily.getOriginTimestamp().substring(0, 10)
                : daily.getOriginTimestamp();
        if (acct.getExpirationDate().compareTo(originDate) < 0) {
            reasonCode = ValidationResult.ACCOUNT_EXPIRED;
            reasonDescription = "TRANSACTION RECEIVED AFTER ACCT EXPIRATION";
        }

        return reasonCode == ValidationResult.OK
                ? ValidationResult.accepted(xref.get(), acct)
                : ValidationResult.rejected(reasonCode, reasonDescription, xref.get(), acct);
    }
}
