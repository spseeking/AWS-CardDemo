package com.carddemo.batch.online.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RACF revoked after a number of failures and cleared the count once the interval passed. */
class SignOnThrottleTest {

    @Test
    void aLockoutEndsAndTheNextSingleFailureDoesNotLockAgain() throws InterruptedException {
        SignOnThrottle throttle = new SignOnThrottle(3, 1);

        for (int attempt = 0; attempt < 3; attempt++) {
            throttle.recordFailure("USER0001");
        }
        assertThat(throttle.isLockedOut("USER0001")).isTrue();

        Thread.sleep(1100);
        assertThat(throttle.isLockedOut("USER0001")).isFalse();

        throttle.recordFailure("USER0001");
        assertThat(throttle.isLockedOut("USER0001"))
                .as("the served lockout starts the count again instead of re-locking at once")
                .isFalse();
    }

    @Test
    void aSuccessfulSignOnClearsTheCount() {
        SignOnThrottle throttle = new SignOnThrottle(2, 300);

        throttle.recordFailure("USER0001");
        throttle.recordSuccess("USER0001");
        throttle.recordFailure("USER0001");

        assertThat(throttle.isLockedOut("USER0001")).isFalse();
    }
}
