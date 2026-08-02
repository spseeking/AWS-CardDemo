package com.carddemo.batch.testsupport;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.Card;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.Customer;
import com.carddemo.batch.domain.TranCategory;
import com.carddemo.batch.domain.TranType;
import com.carddemo.batch.domain.DisclosureGroup;
import com.carddemo.batch.domain.TranCatBalance;
import com.carddemo.batch.domain.TransactionRecord;

import java.math.BigDecimal;

/** Record builders for the converted batch tests. */
public final class Fixtures {

    private Fixtures() {
    }

    public static Account account(long accountId, String groupId, String balance, String creditLimit,
                                  String cycleCredit, String cycleDebit, String expirationDate) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setActiveStatus("Y");
        account.setCurrentBalance(new BigDecimal(balance));
        account.setCreditLimit(new BigDecimal(creditLimit));
        account.setCashCreditLimit(new BigDecimal(creditLimit));
        account.setOpenDate("2014-11-20");
        account.setExpirationDate(expirationDate);
        account.setReissueDate("2025-05-20");
        account.setCurrentCycleCredit(new BigDecimal(cycleCredit));
        account.setCurrentCycleDebit(new BigDecimal(cycleDebit));
        account.setAddressZip("68022");
        account.setGroupId(groupId);
        return account;
    }

    public static CardXref xref(String cardNumber, long customerId, long accountId) {
        return new CardXref(cardNumber, customerId, accountId, "");
    }

    public static TranCatBalance categoryBalance(long accountId, String typeCode, int categoryCode, String balance) {
        return new TranCatBalance(accountId, typeCode, categoryCode, new BigDecimal(balance));
    }

    public static DisclosureGroup disclosureGroup(String groupId, String typeCode, int categoryCode, String rate) {
        return new DisclosureGroup(groupId, typeCode, categoryCode, new BigDecimal(rate), "");
    }

    public static Customer customer(long customerId, String firstName, String lastName, int ficoScore) {
        return new Customer(customerId, firstName, "", lastName, "410 Terry Ave N", "Suite 100", "Seattle",
                "WA", "USA", "99999", "206-555-0100", "", 123456789L, "GOVT-ID", "1980-01-01", "EFT0000001",
                "Y", ficoScore, "");
    }

    public static Card card(String cardNumber, long accountId, String embossedName) {
        return new Card(cardNumber, accountId, 123, embossedName, "2025-05-20", "Y", "");
    }

    public static TranType tranType(String typeCode, String description) {
        return new TranType(typeCode, description, "");
    }

    public static TranCategory tranCategory(String typeCode, int categoryCode, String description) {
        return new TranCategory(typeCode, categoryCode, description, "");
    }

    public static TransactionRecord postedTransaction(String id, String cardNumber, String typeCode,
                                                      int categoryCode, String amount, String processTimestamp) {
        TransactionRecord tran = dailyTransaction(id, cardNumber, typeCode, categoryCode, amount,
                "2022-06-10 19:27:53.000000");
        tran.setProcessTimestamp(processTimestamp);
        return tran;
    }

    public static TransactionRecord dailyTransaction(String id, String cardNumber, String typeCode, int categoryCode,
                                                     String amount, String originTimestamp) {
        TransactionRecord tran = new TransactionRecord();
        tran.setId(id);
        tran.setTypeCode(typeCode);
        tran.setCategoryCode(categoryCode);
        tran.setSource("POS TERM");
        tran.setDescription("Purchase at Abshire-Lowe");
        tran.setAmount(new BigDecimal(amount));
        tran.setMerchantId(800000000L);
        tran.setMerchantName("Abshire-Lowe");
        tran.setMerchantCity("North Enoshaven");
        tran.setMerchantZip("72112");
        tran.setCardNumber(cardNumber);
        tran.setOriginTimestamp(originTimestamp);
        tran.setProcessTimestamp("");
        return tran;
    }
}
