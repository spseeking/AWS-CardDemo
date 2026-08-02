package com.carddemo.batch.online;

import com.carddemo.batch.domain.Card;
import com.carddemo.batch.domain.SecurityUser;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.online.security.SignOnSessions;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** REST entry points replacing the CICS transactions of the CardDemo online programs. */
@RestController
@RequestMapping("/api")
public class OnlineController {

    /** COSGN00C sign on request. */
    public record SignOnRequest(String userId, String password) {
    }

    /** COUSR01C / COUSR02C user maintenance request. */
    public record UserRequest(String userId, String firstName, String lastName, String password, String type) {
    }

    /** CORPT00C report submission request. */
    public record ReportRequest(ReportService.ReportType type, String startDate, String endDate,
                                String confirmation) {
    }

    private final SignOnSessions sessions;
    private final SignOnService signOnService;
    private final MenuService menuService;
    private final UserService userService;
    private final AccountService accountService;
    private final CardService cardService;
    private final TransactionService transactionService;
    private final BillPaymentService billPaymentService;
    private final ReportService reportService;

    public OnlineController(SignOnSessions sessions, SignOnService signOnService, MenuService menuService, UserService userService,
                            AccountService accountService, CardService cardService,
                            TransactionService transactionService, BillPaymentService billPaymentService,
                            ReportService reportService) {
        this.sessions = sessions;
        this.signOnService = signOnService;
        this.menuService = menuService;
        this.userService = userService;
        this.accountService = accountService;
        this.cardService = cardService;
        this.transactionService = transactionService;
        this.billPaymentService = billPaymentService;
        this.reportService = reportService;
    }

    @PostMapping("/signon")
    public SignOnService.SignOnResult signOn(@RequestBody SignOnRequest request) {
        return signOnService.signOn(request.userId(), request.password());
    }

    /** CESF LOGOFF, the terminal sign off that ended the CICS session. */
    @PostMapping("/signoff")
    public void signOff(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        sessions.close(SignOnSessions.token(authorization));
    }

    @GetMapping("/menu")
    public List<MenuService.MenuOption> menu(Authentication authentication) {
        return menuService.menuFor(userType(authentication));
    }

    @GetMapping("/menu/{option}")
    public MenuService.MenuOption menuOption(@PathVariable int option, Authentication authentication) {
        return menuService.select(userType(authentication), option);
    }

    /** COMEN01C read the user type out of the commarea the sign on transaction filled in. */
    private static String userType(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> SignOnSessions.ADMIN_AUTHORITY.equals(authority.getAuthority()));
        return admin ? SignOnService.ADMIN_TYPE : "U";
    }

    @GetMapping("/users")
    public List<SecurityUser> users() {
        return userService.list();
    }

    @GetMapping("/users/{userId}")
    public SecurityUser user(@PathVariable String userId) {
        return userService.find(userId);
    }

    @PostMapping("/users")
    public SecurityUser addUser(@RequestBody UserRequest request) {
        return userService.add(request.userId(), request.firstName(), request.lastName(), request.password(),
                request.type());
    }

    @PutMapping("/users/{userId}")
    public SecurityUser updateUser(@PathVariable String userId, @RequestBody UserRequest request) {
        return userService.update(userId, request.firstName(), request.lastName(), request.password(),
                request.type());
    }

    @DeleteMapping("/users/{userId}")
    public Map<String, String> deleteUser(@PathVariable String userId) {
        userService.delete(userId);
        return Map.of("message", "User " + userId.trim() + " has been deleted ...");
    }

    @GetMapping("/accounts/{accountId}")
    public AccountService.AccountView account(@PathVariable String accountId) {
        return accountService.view(accountId);
    }

    @PutMapping("/accounts/{accountId}")
    public Map<String, Object> updateAccount(@PathVariable String accountId,
                                             @RequestBody AccountService.AccountUpdate request) {
        return Map.of("account", accountService.update(accountId, request));
    }

    @GetMapping("/cards")
    public List<Card> cards(@RequestParam(required = false) String accountId) {
        return cardService.list(accountId);
    }

    @GetMapping("/cards/{cardNumber}")
    public Card card(@PathVariable String cardNumber) {
        return cardService.view(cardNumber);
    }

    @PutMapping("/cards/{cardNumber}")
    public Card updateCard(@PathVariable String cardNumber, @RequestBody CardService.CardUpdate request) {
        return cardService.update(cardNumber, request);
    }

    @GetMapping("/transactions")
    public List<TransactionRecord> transactions() {
        return transactionService.list();
    }

    @GetMapping("/transactions/{transactionId}")
    public TransactionRecord transaction(@PathVariable String transactionId) {
        return transactionService.view(transactionId);
    }

    @PostMapping("/transactions")
    public TransactionRecord addTransaction(@RequestBody TransactionService.NewTransaction request) {
        return transactionService.add(request);
    }

    @GetMapping("/billpay/{accountId}")
    public Map<String, BigDecimal> billPayBalance(@PathVariable String accountId) {
        return Map.of("currentBalance", billPaymentService.balance(accountId));
    }

    @PostMapping("/billpay/{accountId}")
    public BillPaymentService.BillPaymentResult billPay(@PathVariable String accountId,
                                                        @RequestParam(defaultValue = "") String confirmation) {
        return billPaymentService.pay(accountId, confirmation);
    }

    @PostMapping("/reports")
    public ReportService.ReportSubmission submitReport(@RequestBody ReportRequest request) {
        return reportService.submit(request.type(), request.startDate(), request.endDate(),
                request.confirmation());
    }
}
