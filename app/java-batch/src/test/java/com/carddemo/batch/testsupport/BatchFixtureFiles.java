package com.carddemo.batch.testsupport;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.Card;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.Customer;
import com.carddemo.batch.domain.TranCategory;
import com.carddemo.batch.domain.TranType;
import com.carddemo.batch.domain.DisclosureGroup;
import com.carddemo.batch.domain.SecurityUser;
import com.carddemo.batch.domain.TranCatBalance;
import com.carddemo.batch.domain.TransactionRecord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Writes fixed-width input files for the job integration tests. */
public final class BatchFixtureFiles {

    public static final String CARD_A = "4859452612877065";
    public static final String CARD_B = "4859452612877066";
    public static final String CARD_C = "4859452612877067";
    public static final String UNKNOWN_CARD = "9999999999999999";
    public static final String ORIGIN = "2022-06-10 19:27:53.000000";

    private BatchFixtureFiles() {
    }

    public static Path createDirectory() {
        try {
            return Files.createTempDirectory("carddemo-batch");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writePostTranData(Path directory) {
        write(directory.resolve("acctdata.txt"), List.of(
                Fixtures.account(11L, "A000000001", "100.00", "500.00", "0.00", "0.00", "2025-05-20"),
                Fixtures.account(12L, "A000000001", "0.00", "100.00", "0.00", "0.00", "2025-05-20"),
                Fixtures.account(13L, "A000000001", "0.00", "5000.00", "0.00", "0.00", "2021-01-31")),
                Account::format);

        write(directory.resolve("cardxref.txt"), List.of(
                Fixtures.xref(CARD_A, 27L, 11L),
                Fixtures.xref(CARD_B, 28L, 99L),
                Fixtures.xref(CARD_C, 29L, 12L),
                Fixtures.xref("4859452612877068", 30L, 13L)),
                CardXref::format);

        write(directory.resolve("tcatbal.txt"), List.of(
                Fixtures.categoryBalance(11L, "01", 1, "10.00")),
                TranCatBalance::format);

        write(directory.resolve("dailytran.txt"), List.of(
                Fixtures.dailyTransaction("TRAN-OK-1", CARD_A, "01", 1, "50.47", ORIGIN),
                Fixtures.dailyTransaction("TRAN-REJ-100", UNKNOWN_CARD, "01", 1, "10.00", ORIGIN),
                Fixtures.dailyTransaction("TRAN-REJ-101", CARD_B, "01", 1, "10.00", ORIGIN),
                Fixtures.dailyTransaction("TRAN-REJ-102", CARD_C, "01", 1, "500.00", ORIGIN),
                Fixtures.dailyTransaction("TRAN-REJ-103", "4859452612877068", "01", 1, "10.00", ORIGIN),
                Fixtures.dailyTransaction("TRAN-OK-2", CARD_A, "02", 2, "-20.00", ORIGIN)),
                TransactionRecord::format);
    }

    public static void writeIntCalcData(Path directory) {
        write(directory.resolve("acctdata.txt"), List.of(
                Fixtures.account(11L, "A000000001", "100.00", "5000.00", "5.00", "3.00", "2025-05-20"),
                Fixtures.account(12L, "NOSUCHGRP", "200.00", "5000.00", "7.00", "1.00", "2025-05-20")),
                Account::format);

        write(directory.resolve("cardxref.txt"), List.of(
                Fixtures.xref(CARD_A, 27L, 11L),
                Fixtures.xref(CARD_C, 29L, 12L)),
                CardXref::format);

        write(directory.resolve("discgrp.txt"), List.of(
                Fixtures.disclosureGroup("A000000001", "01", 1, "15.00"),
                Fixtures.disclosureGroup("A000000001", "02", 2, "0.00"),
                Fixtures.disclosureGroup("DEFAULT", "01", 1, "12.00")),
                DisclosureGroup::format);

        write(directory.resolve("tcatbal.txt"), List.of(
                Fixtures.categoryBalance(11L, "01", 1, "1000.00"),
                Fixtures.categoryBalance(11L, "02", 2, "500.00"),
                Fixtures.categoryBalance(12L, "01", 1, "1000.00")),
                TranCatBalance::format);
    }

    /** Posted transactions, customers, cards and the lookup files the report and statement jobs read. */
    public static void writeReportingData(Path directory) {
        write(directory.resolve("acctdata.txt"), List.of(
                Fixtures.account(11L, "A000000001", "194.00", "5000.00", "0.00", "0.00", "2025-05-20"),
                Fixtures.account(12L, "A000000001", "-50.00", "5000.00", "0.00", "0.00", "2025-05-20")),
                Account::format);

        write(directory.resolve("cardxref.txt"), List.of(
                Fixtures.xref(CARD_A, 27L, 11L),
                Fixtures.xref(CARD_C, 29L, 12L)),
                CardXref::format);

        write(directory.resolve("custdata.txt"), List.of(
                Fixtures.customer(27L, "Ada", "Lovelace", 780),
                Fixtures.customer(29L, "Alan", "Turing", 690)),
                Customer::format);

        write(directory.resolve("carddata.txt"), List.of(
                Fixtures.card(CARD_A, 11L, "ADA LOVELACE"),
                Fixtures.card(CARD_C, 12L, "ALAN TURING")),
                Card::format);

        write(directory.resolve("trantype.txt"), List.of(
                Fixtures.tranType("01", "Purchase"),
                Fixtures.tranType("02", "Payment")),
                TranType::format);

        write(directory.resolve("trancatg.txt"), List.of(
                Fixtures.tranCategory("01", 1, "Regular Sales Draft"),
                Fixtures.tranCategory("02", 2, "Payment Received")),
                TranCategory::format);

        write(directory.resolve("transact.txt"), List.of(
                Fixtures.postedTransaction("TRAN-A-1", CARD_A, "01", 1, "100.00", "2022-06-11 01:00:00.000000"),
                Fixtures.postedTransaction("TRAN-A-2", CARD_A, "02", 2, "-25.00", "2022-06-12 01:00:00.000000"),
                Fixtures.postedTransaction("TRAN-C-1", CARD_C, "01", 1, "10.00", "2022-06-13 01:00:00.000000"),
                Fixtures.postedTransaction("TRAN-OLD", CARD_C, "01", 1, "999.00", "2021-01-01 01:00:00.000000")),
                TransactionRecord::format);

        writeText(directory.resolve("dateparm.txt"), "2022-06-01 2022-06-30");
    }

    /** The reporting data plus the USRSEC records the online sign on and admin screens need. */
    public static void writeOnlineData(Path directory) {
        writeReportingData(directory);
        write(directory.resolve("usrsec.txt"), List.of(
                new SecurityUser("ADMIN001", "Admin", "User", "PASSWORD", SecurityUser.TYPE_ADMIN, ""),
                new SecurityUser("USER0001", "Regular", "User", "PASSWORD", SecurityUser.TYPE_USER, ""),
                new SecurityUser("USER0002", "Second", "User", "PASSWORD", SecurityUser.TYPE_USER, "")),
                SecurityUser::format);
    }

    public static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.ISO_8859_1).stream()
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeText(Path path, String content) {
        try {
            Files.writeString(path, content + System.lineSeparator(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static <T> void write(Path path, List<T> records, Function<T, String> formatter) {
        String content = records.stream().map(formatter).collect(Collectors.joining(System.lineSeparator()))
                + System.lineSeparator();
        try {
            Files.writeString(path, content, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
