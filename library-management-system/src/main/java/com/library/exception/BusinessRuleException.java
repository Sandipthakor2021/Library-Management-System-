package com.library.exception;

/** Raised when an operation violates a library rule (loan limit, copy unavailable, invalid state...). */
public class BusinessRuleException extends LibraryException {
    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
