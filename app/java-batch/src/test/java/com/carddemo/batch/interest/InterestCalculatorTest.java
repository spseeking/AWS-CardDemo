package com.carddemo.batch.interest;

import com.carddemo.batch.domain.DisclosureGroup;
import com.carddemo.batch.testsupport.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** CBACT04C interest rate lookup and the monthly interest formula. */
class InterestCalculatorTest {

    private final InterestCalculator calculator = new InterestCalculator();

    @ParameterizedTest
    @CsvSource({
            "1000.00, 12.00, 10.00",
            "1000.00, 15.00, 12.50",
            "0.00,    15.00, 0.00",
            "-500.00, 24.00, -10.00"
    })
    void computesMonthlyInterestAsBalanceTimesRateOver1200(String balance, String rate, String expected) {
        assertThat(calculator.monthlyInterest(new BigDecimal(balance), new BigDecimal(rate)))
                .isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    void truncatesMonthlyInterestToTwoDecimalsLikeTheCobolReceivingField() {
        // 100.05 * 13.37 / 1200 = 1.11472... -> S9(09)V99 without ROUNDED keeps 1.11
        assertThat(calculator.monthlyInterest(new BigDecimal("100.05"), new BigDecimal("13.37")))
                .isEqualByComparingTo("1.11");
    }

    @Test
    void usesTheAccountGroupRateWhenPresent() {
        Map<String, DisclosureGroup> groups = Map.of(
                DisclosureGroupKey.of("A000000001", "01", 1), Fixtures.disclosureGroup("A000000001", "01", 1, "15.00"),
                DisclosureGroupKey.of("DEFAULT", "01", 1), Fixtures.disclosureGroup("DEFAULT", "01", 1, "9.99"));

        Optional<DisclosureGroup> group = calculator.findRate("A000000001", "01", 1,
                key -> Optional.ofNullable(groups.get(key)));

        assertThat(group).map(DisclosureGroup::interestRate).hasValue(new BigDecimal("15.00"));
    }

    @Test
    void fallsBackToTheDefaultGroupWhenTheAccountGroupHasNoRate() {
        Map<String, DisclosureGroup> groups = Map.of(
                DisclosureGroupKey.of("DEFAULT", "01", 1), Fixtures.disclosureGroup("DEFAULT", "01", 1, "9.99"));

        Optional<DisclosureGroup> group = calculator.findRate("NOSUCHGRP", "01", 1,
                key -> Optional.ofNullable(groups.get(key)));

        assertThat(group).map(DisclosureGroup::accountGroupId).hasValue("DEFAULT");
        assertThat(group).map(DisclosureGroup::interestRate).hasValue(new BigDecimal("9.99"));
    }

    @Test
    void returnsNoRateWhenNeitherTheGroupNorTheDefaultExists() {
        assertThat(calculator.findRate("NOSUCHGRP", "07", 99, key -> Optional.empty())).isEmpty();
    }
}
