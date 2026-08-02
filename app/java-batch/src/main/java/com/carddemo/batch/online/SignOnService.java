package com.carddemo.batch.online;

import com.carddemo.batch.domain.SecurityUser;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** COSGN00C: validates the sign on fields against USRSEC and picks the landing program. */
@Service
public class SignOnService {

    public static final String ADMIN_TYPE = SecurityUser.TYPE_ADMIN;

    /** What the COSGN00C XCTL would have transferred to, plus the commarea user identity. */
    public record SignOnResult(String userId, String userType, String program) {
    }

    private final CardDemoRepository repository;

    public SignOnService(CardDemoRepository repository) {
        this.repository = repository;
    }

    public SignOnResult signOn(String userId, String password) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessRuleException("Please enter User ID ...");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("Please enter Password ...");
        }

        String key = userId.toUpperCase(Locale.ROOT).trim();
        SecurityUser user = repository.users().find(key)
                .orElseThrow(() -> new BusinessRuleException("User not found. Try again ..."));
        if (!user.password().trim().equals(password.toUpperCase(Locale.ROOT).trim())) {
            throw new BusinessRuleException("Wrong Password. Try again ...");
        }

        return new SignOnResult(user.userId().trim(), user.type().trim(),
                user.isAdmin() ? "COADM01C" : "COMEN01C");
    }
}
