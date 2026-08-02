package com.carddemo.batch.online.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Failed sign on counter, the equivalent of the RACF revoke count CICS relied on. */
@Component
public class SignOnThrottle {

    private record Attempts(int failures, Instant lockedUntil) {
    }

    private final int limit;
    private final Duration lockout;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public SignOnThrottle(@Value("${carddemo.security.max-sign-on-attempts:5}") int limit,
                          @Value("${carddemo.security.lockout-seconds:300}") long lockoutSeconds) {
        this.limit = limit;
        this.lockout = Duration.ofSeconds(lockoutSeconds);
    }

    public boolean isLockedOut(String userId) {
        Attempts current = attempts.get(userId);
        return current != null && current.lockedUntil() != null && current.lockedUntil().isAfter(Instant.now());
    }

    public void recordFailure(String userId) {
        attempts.compute(userId, (key, current) -> {
            int failures = (current == null ? 0 : current.failures()) + 1;
            return new Attempts(failures, failures >= limit ? Instant.now().plus(lockout) : null);
        });
    }

    public void recordSuccess(String userId) {
        attempts.remove(userId);
    }
}
