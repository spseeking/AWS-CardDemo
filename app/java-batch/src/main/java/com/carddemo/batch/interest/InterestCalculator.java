package com.carddemo.batch.interest;

import com.carddemo.batch.domain.DisclosureGroup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.function.Function;

/** CBACT04C 1200-GET-INTEREST-RATE and 1300-COMPUTE-INTEREST. */
public class InterestCalculator {

    private static final BigDecimal MONTHS_TIMES_PERCENT = BigDecimal.valueOf(1200);

    /**
     * Monthly interest for one transaction category balance:
     * {@code (balance * annual rate) / 1200}, truncated to two decimals because the COBOL
     * receiving field is {@code PIC S9(09)V99} and the COMPUTE is not ROUNDED.
     */
    public BigDecimal monthlyInterest(BigDecimal categoryBalance, BigDecimal annualRatePercent) {
        return categoryBalance.multiply(annualRatePercent)
                .divide(MONTHS_TIMES_PERCENT, 2, RoundingMode.DOWN);
    }

    /**
     * Rate lookup for an (account group, transaction type, transaction category) key, falling back
     * to the DEFAULT account group when the account's own group has no disclosure record.
     */
    public Optional<DisclosureGroup> findRate(String accountGroupId, String transactionTypeCode,
                                              int transactionCategoryCode,
                                              Function<String, Optional<DisclosureGroup>> lookup) {
        Optional<DisclosureGroup> group = lookup.apply(
                DisclosureGroupKey.of(accountGroupId, transactionTypeCode, transactionCategoryCode));
        if (group.isPresent()) {
            return group;
        }
        return lookup.apply(DisclosureGroupKey.of(DisclosureGroup.DEFAULT_GROUP_ID, transactionTypeCode,
                transactionCategoryCode));
    }
}
