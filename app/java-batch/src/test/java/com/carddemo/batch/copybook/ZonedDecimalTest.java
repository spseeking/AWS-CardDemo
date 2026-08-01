package com.carddemo.batch.copybook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZonedDecimalTest {

    @ParameterizedTest
    @CsvSource({
            "00000000000{, 0.00",
            "0000005047G, 504.77",
            "0000009190}, -919.00",
            "00000001940{, 194.00",
            "00150{, 15.00",
            "0000000001R, -0.19"
    })
    void parsesOverpunchedSign(String field, String expected) {
        assertThat(ZonedDecimal.parse(field, 2)).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    void parsesUnsignedDigits() {
        assertThat(ZonedDecimal.parse("00000012345", 2)).isEqualByComparingTo("123.45");
    }

    @ParameterizedTest
    @CsvSource({
            "504.77, 0000005047G",
            "-919.00, 0000009190}",
            "0.00, 0000000000{",
            "-0.19, 0000000001R"
    })
    void formatsOverpunchedSign(String value, String expected) {
        assertThat(ZonedDecimal.format(new BigDecimal(value), 11, 2)).isEqualTo(expected);
    }

    @Test
    void roundTripsEverySampleAccountAmount() {
        String amount = "00000061300{";
        assertThat(ZonedDecimal.format(ZonedDecimal.parse(amount, 2), 12, 2)).isEqualTo(amount);
    }

    @Test
    void truncatesRatherThanRoundsExtraDecimals() {
        assertThat(ZonedDecimal.format(new BigDecimal("1.999"), 11, 2)).isEqualTo("0000000019I");
    }

    @Test
    void rejectsNonZonedField() {
        assertThatThrownBy(() -> ZonedDecimal.parse("12345*", 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
