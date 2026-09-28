package com.library.exception;

/** Base type for all domain errors raised by the library system. */
public class LibraryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LibraryException(String message) {
        super(message);
    }
}
