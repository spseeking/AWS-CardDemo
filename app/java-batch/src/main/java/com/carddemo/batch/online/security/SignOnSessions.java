package com.carddemo.batch.online.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The CICS commarea identity, carried between requests as a bearer token instead. A token is what
 * the sign on transaction handed the terminal session; nothing else establishes an identity.
 */
@Component
public class SignOnSessions {

    public static final String ADMIN_AUTHORITY = "ROLE_ADMIN";
    public static final String USER_AUTHORITY = "ROLE_USER";
    private static final String BEARER = "Bearer ";

    /** The token out of an {@code Authorization} header, or an empty string when there is none. */
    public static String token(String authorizationHeader) {
        return authorizationHeader != null && authorizationHeader.startsWith(BEARER)
                ? authorizationHeader.substring(BEARER.length()).trim()
                : "";
    }

    private record Session(String userId, String userType, Instant expiresAt) {
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Duration timeout;

    public SignOnSessions(@Value("${carddemo.security.session-timeout-seconds:1800}") long timeoutSeconds) {
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public String open(String userId, String userType) {
        sessions.values().removeIf(session -> session.expiresAt().isBefore(Instant.now()));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(userId, userType, Instant.now().plus(timeout)));
        return token;
    }

    public void close(String token) {
        if (!token.isEmpty()) {
            sessions.remove(token);
        }
    }

    public Optional<Authentication> authenticate(String token) {
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        String authority = "A".equalsIgnoreCase(session.userType()) ? ADMIN_AUTHORITY : USER_AUTHORITY;
        return Optional.of(UsernamePasswordAuthenticationToken.authenticated(session.userId(), null,
                List.of(new SimpleGrantedAuthority(authority))));
    }
}
