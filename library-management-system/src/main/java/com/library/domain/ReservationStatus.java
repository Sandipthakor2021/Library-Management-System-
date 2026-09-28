package com.library.domain;

public enum ReservationStatus {
    /** Waiting in the queue for a copy. */
    PENDING,
    /** A copy is on the hold shelf and the patron has been notified. */
    READY,
    FULFILLED,
    CANCELLED
}
