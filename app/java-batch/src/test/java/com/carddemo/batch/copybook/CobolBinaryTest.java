package com.carddemo.batch.copybook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CobolBinaryTest {

    @Test
    void sizesFieldsTheWayIbmCobolAllocatesThem() {
        assertThat(CobolBinary.byteLength(3)).isEqualTo(2);
        assertThat(CobolBinary.byteLength(9)).isEqualTo(4);
        assertThat(CobolBinary.byteLength(11)).isEqualTo(8);
    }

    @Test
    void storesBigEndianAndRoundTrips() {
        byte[] bytes = CobolBinary.format(305419896L, 9);

        assertThat(bytes).containsExactly(0x12, 0x34, 0x56, 0x78);
        assertThat(CobolBinary.parse(bytes, 0, 9)).isEqualTo(305419896L);
    }

    @Test
    void keepsNegativeValuesSigned() {
        byte[] bytes = CobolBinary.format(-1234L, 4);

        assertThat(CobolBinary.parse(bytes, 0, 4)).isEqualTo(-1234L);
    }
}
