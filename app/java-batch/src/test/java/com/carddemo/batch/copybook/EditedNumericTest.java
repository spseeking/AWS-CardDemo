package com.carddemo.batch.copybook;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EditedNumericTest {

    @Test
    void suppressesLeadingZerosAndGroupsThousands() {
        assertThat(EditedNumeric.signedGrouped(new BigDecimal("1234.5")))
                .isEqualTo("       1,234.50")
                .hasSize(15);
    }

    @Test
    void printsTheMinusSignOnlyWhenNegative() {
        assertThat(EditedNumeric.signedGrouped(new BigDecimal("-9.99"))).startsWith("-");
        assertThat(EditedNumeric.signedGrouped(new BigDecimal("9.99"))).startsWith(" ");
    }

    @Test
    void alwaysPrintsASignForTheTotalLines() {
        assertThat(EditedNumeric.plusSignedGrouped(new BigDecimal("0"))).isEqualTo("+          0.00");
        assertThat(EditedNumeric.plusSignedGrouped(new BigDecimal("-2.5"))).isEqualTo("-          2.50");
    }

    @Test
    void writesTrailingSignAmountsForTheStatement() {
        assertThat(EditedNumeric.zeroFilledTrailingSign(new BigDecimal("-194")))
                .isEqualTo("000000194.00-")
                .hasSize(13);
        assertThat(EditedNumeric.zeroSuppressedTrailingSign(new BigDecimal("25.5")))
                .isEqualTo("       25.50 ")
                .hasSize(13);
        assertThat(EditedNumeric.zeroSuppressedTrailingSign(BigDecimal.ZERO)).isEqualTo("         .00 ");
    }
}
