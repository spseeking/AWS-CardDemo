package com.carddemo.batch.posting;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.testsupport.Fixtures;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Reject reason codes produced by CBTRN02C 1500-VALIDATE-TRAN. */
class TransactionValidatorTest {

    private static final String CARD = "4859452612877065";
    private static final String ORIGIN = "2022-06-10 19:27:53.000000";

    private final TransactionValidator validator = new TransactionValidator();

    private final CardXref xref = Fixtures.xref(CARD, 27L, 11L);

    private ValidationResult validate(TransactionRecord daily, CardXref knownXref, Account knownAccount) {
        return validator.validate(daily,
                cardNumber -> Optional.ofNullable(knownXref).filter(x -> x.cardNumber().equals(cardNumber)),
                accountId -> Optional.ofNullable(knownAccount).filter(a -> a.getAccountId() == accountId));
    }

    @Test
    void acceptsTransactionWithinCreditLimit() {
        Account account = Fixtures.account(11L, "ZEROAPR", "100.00", "5000.00", "0.00", "0.00", "2025-05-20");
        TransactionRecord daily = Fixtures.dailyTransaction("T1", CARD, "01", 1, "50.47", ORIGIN);

        ValidationResult result = validate(daily, xref, account);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.reasonCode()).isEqualTo(ValidationResult.OK);
        assertThat(result.account()).isSameAs(account);
    }

    @Test
    void rejects100WhenCardIsNotCrossReferenced() {
        TransactionRecord daily = Fixtures.dailyTransaction("T1", "9999999999999999", "01", 1, "50.47", ORIGIN);

        ValidationResult result = validate(daily, xref, null);

        assertThat(result.reasonCode()).isEqualTo(100);
        assertThat(result.reasonDescription()).isEqualTo("INVALID CARD NUMBER FOUND");
    }

    @Test
    void rejects101WhenAccountIsMissing() {
        TransactionRecord daily = Fixtures.dailyTransaction("T1", CARD, "01", 1, "50.47", ORIGIN);

        ValidationResult result = validate(daily, xref, null);

        assertThat(result.reasonCode()).isEqualTo(101);
        assertThat(result.reasonDescription()).isEqualTo("ACCOUNT RECORD NOT FOUND");
    }

    @Test
    void rejects102WhenCycleActivityPlusAmountExceedsCreditLimit() {
        Account account = Fixtures.account(11L, "ZEROAPR", "0.00", "500.00", "400.00", "50.00", "2025-05-20");
        TransactionRecord daily = Fixtures.dailyTransaction("T1", CARD, "01", 1, "151.00", ORIGIN);

        ValidationResult result = validate(daily, xref, account);

        assertThat(result.reasonCode()).isEqualTo(102);
        assertThat(result.reasonDescription()).isEqualTo("OVERLIMIT TRANSACTION");
    }

    @Test
    void acceptsTransactionExactlyAtCreditLimit() {
        Account account = Fixtures.account(11L, "ZEROAPR", "0.00", "500.00", "400.00", "50.00", "2025-05-20");
        TransactionRecord daily = Fixtures.dailyTransaction("T1", CARD, "01", 1, "150.00", ORIGIN);

        assertThat(validate(daily, xref, account).isAccepted()).isTrue();
    }

    @Test
    void rejects103WhenTransactionIsAfterAccountExpiry() {
        Account account = Fixtures.account(11L, "ZEROAPR", "0.00", "5000.00", "0.00", "0.00", "2021-01-31");
        TransactionRecord daily = Fixtures.dailyTransaction("T1", CARD, "01", 1, "50.47", ORIGIN);

        ValidationResult result = validate(daily, xref, account);

        assertThat(result.reasonCode()).isEqualTo(103);
        assertThat(result.reasonDescription()).isEqualTo("TRANSACTION RECEIVED AFTER ACCT EXPIRATION");
    }

    @Test
    void expiryReasonOverridesOverlimitWhenBothFail() {
        Account account = Fixtures.account(11L, "ZEROAPR", "0.00", "100.00", "0.00", "0.00", "2021-01-31");
        TransactionRecord daily = Fixtures.dailyTransaction("T1", CARD, "01", 1, "500.00", ORIGIN);

        assertThat(validate(daily, xref, account).reasonCode()).isEqualTo(103);
    }
}
