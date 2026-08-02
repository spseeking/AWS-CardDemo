package com.carddemo.batch.online;

import com.carddemo.batch.domain.SecurityUser;
import com.carddemo.batch.online.security.PasswordVault;
import com.carddemo.batch.online.security.SignOnSessions;
import com.carddemo.batch.online.security.SignOnThrottle;
import com.carddemo.batch.store.KeyedRecordStore;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** COSGN00C: validates the sign on fields against USRSEC and picks the landing program. */
@Service
public class SignOnService {

    public static final String ADMIN_TYPE = SecurityUser.TYPE_ADMIN;

    /** What the COSGN00C XCTL would have transferred to, plus the commarea user identity. */
    public record SignOnResult(String userId, String userType, String program, String token) {
    }

    private final CardDemoRepository repository;
    private final PasswordVault vault;
    private final SignOnThrottle throttle;
    private final SignOnSessions sessions;

    public SignOnService(CardDemoRepository repository, PasswordVault vault, SignOnThrottle throttle,
                         SignOnSessions sessions) {
        this.repository = repository;
        this.vault = vault;
        this.throttle = throttle;
        this.sessions = sessions;
    }

    public SignOnResult signOn(String userId, String password) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessRuleException("Please enter User ID ...");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("Please enter Password ...");
        }

        String key = userId.toUpperCase(Locale.ROOT).trim();
        if (throttle.isLockedOut(key)) {
            throw new BusinessRuleException("User is locked out. Contact your administrator ...");
        }
        SecurityUser user = repository.users().find(key)
                .orElseThrow(() -> {
                    throttle.recordFailure(key);
                    return new BusinessRuleException("User not found. Try again ...");
                });
        String submitted = password.toUpperCase(Locale.ROOT).trim();
        if (!verify(user, submitted)) {
            throttle.recordFailure(key);
            throw new BusinessRuleException("Wrong Password. Try again ...");
        }
        throttle.recordSuccess(key);

        String signedOn = user.userId().trim();
        return new SignOnResult(signedOn, user.type().trim(), user.isAdmin() ? "COADM01C" : "COMEN01C",
                sessions.open(signedOn, user.type().trim()));
    }

    /**
     * A hash always wins. A user still carrying a clear SEC-USR-PWD from the mainframe extract is
     * migrated on this first successful sign on and the clear field blanked, so the legacy password
     * survives exactly one use.
     */
    private boolean verify(SecurityUser user, String submitted) {
        String key = user.userId().trim();
        if (vault.hasHash(key)) {
            return vault.matches(key, submitted);
        }
        if (user.password().trim().isEmpty() || !user.password().trim().equals(submitted)) {
            return false;
        }
        vault.store(key, submitted);
        KeyedRecordStore<SecurityUser> users = repository.users();
        users.put(new SecurityUser(user.userId(), user.firstName(), user.lastName(), "", user.type(),
                user.filler()));
        users.save();
        return true;
    }
}
