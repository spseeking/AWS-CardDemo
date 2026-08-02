package com.carddemo.batch.domain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every copybook codec must reproduce the repository's sample records byte for byte, otherwise
 * the converted jobs cannot be dual-run against the mainframe output.
 */
class CopybookRoundTripTest {

    @Test
    void accountRecordsRoundTrip() {
        assertRoundTrip("sample/acctdata.txt", Account.LENGTH, record -> Account.parse(record).format());
    }

    @Test
    void cardXrefRecordsRoundTrip() {
        assertRoundTrip("sample/cardxref.txt", CardXref.LENGTH, record -> CardXref.parse(record).format());
    }

    @Test
    void dailyTransactionRecordsRoundTrip() {
        assertRoundTrip("sample/dailytran.txt", TransactionRecord.LENGTH,
                record -> TransactionRecord.parse(record).format());
    }

    @Test
    void disclosureGroupRecordsRoundTrip() {
        assertRoundTrip("sample/discgrp.txt", DisclosureGroup.LENGTH,
                record -> DisclosureGroup.parse(record).format());
    }

    @Test
    void customerRecordsRoundTrip() {
        assertRoundTrip("sample/custdata.txt", Customer.LENGTH, record -> Customer.parse(record).format());
    }

    @Test
    void cardRecordsRoundTrip() {
        assertRoundTrip("sample/carddata.txt", Card.LENGTH, record -> Card.parse(record).format());
    }

    @Test
    void transactionTypeRecordsRoundTrip() {
        assertRoundTrip("sample/trantype.txt", TranType.LENGTH, record -> TranType.parse(record).format());
    }

    @Test
    void transactionCategoryRecordsRoundTrip() {
        assertRoundTrip("sample/trancatg.txt", TranCategory.LENGTH,
                record -> TranCategory.parse(record).format());
    }

    @Test
    void categoryBalanceRecordsRoundTrip() {
        assertRoundTrip("sample/tcatbal.txt", TranCatBalance.LENGTH,
                record -> TranCatBalance.parse(record).format());
    }

    @Test
    void parsesSampleAccountFields() {
        Account account = Account.parse(readLines("sample/acctdata.txt").get(0));

        assertThat(account.getAccountId()).isEqualTo(1L);
        assertThat(account.getActiveStatus()).isEqualTo("Y");
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("194.00");
        assertThat(account.getCreditLimit()).isEqualByComparingTo("2020.00");
        assertThat(account.getExpirationDate()).isEqualTo("2025-05-20");
        // The shipped sample file leaves ACCT-ADDR-ZIP empty and starts the group id ten bytes
        // early, so by copybook offsets the group id lands in the zip field.
        assertThat(account.getAddressZip().trim()).isEqualTo("A000000000");
        assertThat(account.getGroupId().trim()).isEmpty();
    }

    private void assertRoundTrip(String resource, int expectedLength, Function<String, String> codec) {
        List<String> lines = readLines(resource);
        assertThat(lines).isNotEmpty();
        for (String line : lines) {
            String padded = line.length() >= expectedLength
                    ? line.substring(0, expectedLength)
                    : line + " ".repeat(expectedLength - line.length());
            assertThat(codec.apply(line)).hasSize(expectedLength).isEqualTo(padded);
        }
    }

    private List<String> readLines(String resource) {
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1).lines()
                    .map(line -> line.replace("\r", ""))
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
