package com.carddemo.batch.online;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.Card;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.testsupport.BatchFixtureFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The converted CICS programs must keep the 3270 validation messages and the file updates. */
@SpringBootTest
class OnlineServicesTest {

    private static final Path DIRECTORY = BatchFixtureFiles.createDirectory();

    @DynamicPropertySource
    static void files(DynamicPropertyRegistry registry) {
        registry.add("carddemo.card-xref-file", () -> DIRECTORY.resolve("cardxref.txt"));
        registry.add("carddemo.account-file", () -> DIRECTORY.resolve("acctdata.txt"));
        registry.add("carddemo.customer-file", () -> DIRECTORY.resolve("custdata.txt"));
        registry.add("carddemo.card-file", () -> DIRECTORY.resolve("carddata.txt"));
        registry.add("carddemo.transaction-file", () -> DIRECTORY.resolve("transact.txt"));
        registry.add("carddemo.user-security-file", () -> DIRECTORY.resolve("usrsec.txt"));
        registry.add("carddemo.user-credential-file", () -> DIRECTORY.resolve("usrsec.hash"));
    }

    @Autowired
    private SignOnService signOnService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private UserService userService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private CardService cardService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private BillPaymentService billPaymentService;

    @BeforeEach
    void writeFiles() {
        BatchFixtureFiles.writeOnlineData(DIRECTORY);
    }

    @Test
    void signOnSendsAdministratorsToTheAdminMenuAndEveryoneElseToTheMainMenu() {
        assertThat(signOnService.signOn("admin001", "password").program()).isEqualTo("COADM01C");
        assertThat(signOnService.signOn("USER0001", "PASSWORD").program()).isEqualTo("COMEN01C");
    }

    @Test
    void signOnRejectsTheSameWayCosgn00cDid() {
        assertThatThrownBy(() -> signOnService.signOn("", "PASSWORD"))
                .hasMessage("Please enter User ID ...");
        assertThatThrownBy(() -> signOnService.signOn("USER0001", " "))
                .hasMessage("Please enter Password ...");
        assertThatThrownBy(() -> signOnService.signOn("NOBODY", "PASSWORD"))
                .hasMessage("User not found. Try again ...");
        assertThatThrownBy(() -> signOnService.signOn("USER0001", "NOPE"))
                .hasMessage("Wrong Password. Try again ...");
    }

    @Test
    void menusMatchTheOptionCopybooks() {
        assertThat(menuService.menuFor("U")).hasSize(11);
        assertThat(menuService.menuFor("A")).hasSize(6);
        assertThat(menuService.select("U", 10).program()).isEqualTo("COBIL00C");
        assertThat(menuService.select("A", 2).program()).isEqualTo("COUSR01C");
        assertThatThrownBy(() -> menuService.select("U", 12))
                .hasMessage("Please enter a valid option number...");
    }

    @Test
    void userMaintenanceAddsUpdatesAndDeletesTheSecurityFile() {
        userService.add("NEWUSER1", "New", "User", "SECRET99", "U");
        assertThat(userService.find("NEWUSER1").firstName().trim()).isEqualTo("New");

        assertThatThrownBy(() -> userService.add("NEWUSER1", "New", "User", "SECRET99", "U"))
                .hasMessage("User ID already exist...");
        assertThatThrownBy(() -> userService.add("NEWUSER2", "", "User", "SECRET99", "U"))
                .hasMessage("First Name can NOT be empty...");
        assertThatThrownBy(() -> userService.update("NEWUSER1", "New", "User", "SECRET99", "U"))
                .hasMessage("Please modify to update ...");

        assertThatThrownBy(() -> userService.add("NEWUSER12", "New", "User", "SECRET99", "U"))
                .as("a longer id would be truncated to 8 bytes on write and overwrite NEWUSER1")
                .hasMessage("User ID must be 8 characters or less...");

        assertThat(userService.update("NEWUSER1", "Renamed", "User", "SECRET99", "A").isAdmin()).isTrue();
        userService.delete("NEWUSER1");
        assertThatThrownBy(() -> userService.find("NEWUSER1")).hasMessage("User ID NOT found...");
    }

    @Test
    void accountViewJoinsTheCrossReferenceAndCustomerRecords() {
        AccountService.AccountView view = accountService.view("11");

        assertThat(view.account().getAccountId()).isEqualTo(11L);
        assertThat(view.customer().fullName()).contains("Ada").contains("Lovelace");
        assertThat(view.cardNumber()).isEqualTo(BatchFixtureFiles.CARD_A);
        assertThatThrownBy(() -> accountService.view("0"))
                .hasMessage("Account number must be a non zero 11 digit number");
        assertThatThrownBy(() -> accountService.view("99")).hasMessage("Account not found in ACCTDAT");
    }

    @Test
    void accountUpdateEnforcesTheCoactupcEditsAndRewritesTheRecord() {
        assertThatThrownBy(() -> accountService.update("11", new AccountService.AccountUpdate("X",
                new BigDecimal("100.00"), new BigDecimal("100.00"), "2030-01-01", "2026-01-01")))
                .hasMessage("Account Active Status must be Y or N");
        assertThatThrownBy(() -> accountService.update("11", new AccountService.AccountUpdate("Y",
                null, new BigDecimal("100.00"), "2030-01-01", "2026-01-01")))
                .hasMessage("Credit Limit must be supplied");
        assertThatThrownBy(() -> accountService.update("11", new AccountService.AccountUpdate("Y",
                new BigDecimal("100.00"), new BigDecimal("100.00"), "2030-13-01", "2026-01-01")))
                .hasMessage("Card expiry month must be between 1 and 12");

        Account updated = accountService.update("11", new AccountService.AccountUpdate("N",
                new BigDecimal("9000.00"), new BigDecimal("900.00"), "2030-01-01", "2026-01-01"));

        assertThat(updated.getCreditLimit()).isEqualByComparingTo("9000.00");
        assertThat(accountService.view("11").account().getActiveStatus().trim()).isEqualTo("N");
    }

    @Test
    void cardListFiltersOnAccountAndUpdatePersistsTheEmbossedName() {
        assertThat(cardService.list(null)).hasSize(2);
        assertThat(cardService.list("11")).extracting(Card::cardNumber)
                .containsExactly(BatchFixtureFiles.CARD_A);

        assertThatThrownBy(() -> cardService.update(BatchFixtureFiles.CARD_A,
                new CardService.CardUpdate("ADA 1", "2030-01-01", "Y")))
                .hasMessage("Card name can only contain alphabets and spaces");

        cardService.update(BatchFixtureFiles.CARD_A, new CardService.CardUpdate("ada lovelace king",
                "2030-01-01", "N"));

        Card reloaded = cardService.view(BatchFixtureFiles.CARD_A);
        assertThat(reloaded.embossedName().trim()).isEqualTo("ADA LOVELACE KING");
        assertThat(reloaded.activeStatus()).isEqualTo("N");
    }

    @Test
    void migratesTheLegacyClearPasswordOutOfUsrsecOnTheFirstSignOn() throws Exception {
        signOnService.signOn("USER0001", "PASSWORD");

        String record = java.nio.file.Files.readAllLines(DIRECTORY.resolve("usrsec.txt")).stream()
                .filter(line -> line.startsWith("USER0001"))
                .findFirst()
                .orElseThrow();

        assertThat(record).hasSize(80);
        assertThat(record.substring(48, 56)).as("SEC-USR-PWD is blanked once the hash is stored")
                .isBlank();
        assertThat(signOnService.signOn("USER0001", "PASSWORD").userId()).isEqualTo("USER0001");
        assertThat(java.nio.file.Files.readString(DIRECTORY.resolve("usrsec.hash")))
                .doesNotContain("PASSWORD");
    }

    @Test
    void locksAUserOutAfterRepeatedWrongPasswords() {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> signOnService.signOn("USER0002", "NOPE"))
                    .hasMessage("Wrong Password. Try again ...");
        }

        assertThatThrownBy(() -> signOnService.signOn("USER0002", "PASSWORD"))
                .hasMessage("User is locked out. Contact your administrator ...");
    }

    @Test
    void resubmittingTheFormOfAUserWhoNeverSignedOnIsStillRefused() {
        // USER0002 never signs on successfully in these tests, so it keeps its clear USRSEC password
        assertThatThrownBy(() -> userService.update("USER0002", "Second", "User", "PASSWORD", "U"))
                .as("no hashed password yet is not a changed password")
                .hasMessage("Please modify to update ...");

        assertThat(userService.update("USER0002", "Second", "User", "NEWPASS1", "U").password().trim())
                .as("a changed password moves out of the clear SEC-USR-PWD field")
                .isEmpty();
    }

    @Test
    void aUserAddedWithALowerCasePasswordCanStillSignOn() {
        userService.add("LOWER001", "Lower", "Case", "secret", "U");

        assertThat(signOnService.signOn("lower001", "secret").program()).isEqualTo("COMEN01C");
    }

    @Test
    void transactionViewPadsTheEnteredIdToTheSixteenByteKey() {
        assertThat(transactionService.view("TRAN-A-1").getId()).hasSize(16);
    }

    @Test
    void transactionAddResolvesTheCardFromTheAccountAndAssignsTheNextId() {
        TransactionRecord added = transactionService.add(new TransactionService.NewTransaction("11", null,
                "01", "1", "POS TERM", "Online purchase", "42.50", "2022-07-01 10:00:00.000000",
                "2022-07-01 10:00:00.000000", "800000000", "Abshire-Lowe", "Seattle", "99999"));

        assertThat(added.getCardNumber()).isEqualTo(BatchFixtureFiles.CARD_A);
        assertThat(added.getId()).hasSize(16);
        assertThat(added.getAmount()).isEqualByComparingTo("42.50");
        assertThat(transactionService.view(added.getId()).getDescription().trim())
                .isEqualTo("Online purchase");
        assertThat(transactionService.list()).hasSize(5);
    }

    @Test
    void transactionAddKeepsTheCotrn02cEdits() {
        assertThatThrownBy(() -> transactionService.add(new TransactionService.NewTransaction(null, null,
                "01", "1", "POS", "d", "1.00", "2022-07-01", "2022-07-01", "1", "m", "c", "z")))
                .hasMessage("Account or Card Number must be entered...");
        assertThatThrownBy(() -> transactionService.add(new TransactionService.NewTransaction("11", null,
                "", "1", "POS", "d", "1.00", "2022-07-01", "2022-07-01", "1", "m", "c", "z")))
                .hasMessage("Type CD can NOT be empty...");
        assertThatThrownBy(() -> transactionService.add(new TransactionService.NewTransaction("11", null,
                "01", "1", "POS", "d", "1", "2022-07-01", "2022-07-01", "1", "m", "c", "z")))
                .hasMessage("Amount should be in format -99999999.99");
        assertThatThrownBy(() -> transactionService.add(new TransactionService.NewTransaction("11", null,
                "01", "1", "POS", "d", "1.00", "07/01/2022", "2022-07-01", "1", "m", "c", "z")))
                .hasMessage("Orig Date should be in format YYYY-MM-DD");
    }

    @Test
    void billPaymentPostsTheWholeBalanceAndZeroesTheAccount() {
        BillPaymentService.BillPaymentResult result = billPaymentService.pay("11", "Y");

        assertThat(result.amountPaid()).isEqualByComparingTo("194.00");
        assertThat(result.currentBalance()).isEqualByComparingTo("0.00");

        TransactionRecord payment = transactionService.view(result.transactionId());
        assertThat(payment.getTypeCode()).isEqualTo("02");
        assertThat(payment.getCategoryCode()).isEqualTo(2);
        assertThat(payment.getDescription().trim()).isEqualTo("BILL PAYMENT - ONLINE");
        assertThat(payment.getMerchantId()).isEqualTo(999999999L);
    }

    @Test
    void billPaymentRefusesWithoutConfirmationOrBalance() {
        assertThatThrownBy(() -> billPaymentService.pay("11", ""))
                .hasMessage("Confirm to make a bill payment...");
        assertThatThrownBy(() -> billPaymentService.pay("11", "X"))
                .hasMessage("Invalid value. Valid values are (Y/N)...");
        assertThatThrownBy(() -> billPaymentService.pay("12", "Y"))
                .hasMessage("You have nothing to pay...");
    }
}
