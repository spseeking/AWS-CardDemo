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

    /** {@code forgetAt} is when the count lapses, and also when a reached limit stops locking. */
    private record Attempts(int failures, Instant forgetAt) {
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
        if (current == null) {
            return false;
        }
        if (lapsed(current)) {
            attempts.remove(userId, current);
            return false;
        }
        return current.failures() >= limit;
    }

    /**
     * RACF cleared the revoke count once the revoke interval passed, so a served lockout starts the
     * count again rather than re-locking on the next single mistyped password.
     */
    public void recordFailure(String userId) {
        sweep();
        attempts.compute(userId, (key, current) -> new Attempts(
                (current == null || lapsed(current) ? 0 : current.failures()) + 1,
                Instant.now().plus(lockout)));
    }

    public void recordSuccess(String userId) {
        attempts.remove(userId);
    }

    /** Ids that never sign on, including ones that do not exist, must not accumulate. */
    private void sweep() {
        attempts.values().removeIf(SignOnThrottle::lapsed);
    }

    private static boolean lapsed(Attempts attempt) {
        return !attempt.forgetAt().isAfter(Instant.now());
    }
}
