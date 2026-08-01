package com.carddemo.batch.online;

/**
 * Carries the message a CICS program would have placed in the map's ERRMSG field, so the REST
 * clients see the same wording as the 3270 screens.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
