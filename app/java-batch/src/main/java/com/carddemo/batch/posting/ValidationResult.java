package com.carddemo.batch.posting;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;

/**
 * Outcome of CBTRN02C's 1500-VALIDATE-TRAN, carrying the reason code written to the reject
 * trailer plus the records looked up along the way so posting does not repeat the reads.
 */
public record ValidationResult(int reasonCode, String reasonDescription, CardXref xref, Account account) {

    public static final int OK = 0;
    public static final int INVALID_CARD_NUMBER = 100;
    public static final int ACCOUNT_NOT_FOUND = 101;
    public static final int OVERLIMIT = 102;
    public static final int ACCOUNT_EXPIRED = 103;

    public static ValidationResult accepted(CardXref xref, Account account) {
        return new ValidationResult(OK, "", xref, account);
    }

    public static ValidationResult rejected(int reasonCode, String reasonDescription, CardXref xref, Account account) {
        return new ValidationResult(reasonCode, reasonDescription, xref, account);
    }

    public boolean isAccepted() {
        return reasonCode == OK;
    }
}
