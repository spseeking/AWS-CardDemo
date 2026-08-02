package com.carddemo.batch.copybook;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PackedDecimalTest {

    @Test
    void packsAnOddDigitFieldWithoutAPaddingNibble() {
        byte[] packed = PackedDecimal.format(new BigDecimal("123.45"), 11, 2);

        assertThat(packed).hasSize(6);
        assertThat(hex(packed)).isEqualTo("00000012345C");
    }

    @Test
    void marksNegativeAmountsWithTheDSignNibble() {
        byte[] packed = PackedDecimal.format(new BigDecimal("-0.01"), 11, 2);

        assertThat(hex(packed)).endsWith("1D");
        assertThat(PackedDecimal.parse(packed, 0, 11, 2)).isEqualByComparingTo("-0.01");
    }

    @Test
    void padsAnEvenDigitFieldWithALeadingZeroNibble() {
        byte[] packed = PackedDecimal.format(new BigDecimal("-12345678.90"), 12, 2);

        assertThat(packed).hasSize(7);
        assertThat(PackedDecimal.parse(packed, 0, 12, 2)).isEqualByComparingTo("-12345678.90");
    }

    @Test
    void roundTripsAtAnOffsetInsideALargerRecord() {
        byte[] record = new byte[100];
        byte[] packed = PackedDecimal.format(new BigDecimal("987654321.99"), 11, 2);
        System.arraycopy(packed, 0, record, 40, packed.length);

        assertThat(PackedDecimal.parse(record, 40, 11, 2)).isEqualByComparingTo("987654321.99");
    }

    @Test
    void truncatesRatherThanRoundsExtraDecimals() {
        byte[] packed = PackedDecimal.format(new BigDecimal("1.999"), 3, 0);

        assertThat(PackedDecimal.parse(packed, 0, 3, 0)).isEqualByComparingTo("1");
    }

    @Test
    void unsignedPicture9FieldsCarryTheFSignNibble() {
        assertThat(hex(PackedDecimal.formatUnsigned(new BigDecimal("780"), 3, 0))).isEqualTo("780F");
        assertThat(hex(PackedDecimal.format(new BigDecimal("780"), 3, 0))).isEqualTo("780C");
        assertThat(PackedDecimal.parse(PackedDecimal.formatUnsigned(new BigDecimal("780"), 3, 0), 0, 3, 0))
                .isEqualByComparingTo("780");
    }

    private static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (byte value : bytes) {
            text.append(String.format("%02X", value));
        }
        return text.toString();
    }
}
