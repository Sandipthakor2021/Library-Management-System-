package com.library.domain;

public enum CopyStatus {
    AVAILABLE,
    BORROWED,
    /** On the hold shelf for a patron who reserved the title. */
    RESERVED,
    IN_TRANSIT
}
