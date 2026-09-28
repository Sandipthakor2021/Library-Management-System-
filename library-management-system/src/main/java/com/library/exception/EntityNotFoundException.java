package com.library.exception;

/** Raised when a book, copy, patron, branch or loan cannot be found. */
public class EntityNotFoundException extends LibraryException {
    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String entity, String id) {
        super(entity + " not found: " + id);
    }
}
