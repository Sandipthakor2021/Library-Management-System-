package com.library.exception;

/** Raised when an entity with the same unique key already exists. */
public class DuplicateEntityException extends LibraryException {
    private static final long serialVersionUID = 1L;

    public DuplicateEntityException(String entity, String key) {
        super(entity + " already exists: " + key);
    }
}
