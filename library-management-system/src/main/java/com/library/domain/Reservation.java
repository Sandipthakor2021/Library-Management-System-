package com.library.domain;

import java.time.Instant;

/** A patron's place in the queue for a title whose copies are all checked out. */
public class Reservation {
    private final String id;
    private final String isbn;
    private final String patronId;
    private final Instant createdAt;
    private ReservationStatus status = ReservationStatus.PENDING;
    private String heldCopyId;

    public Reservation(String id, String isbn, String patronId, Instant createdAt) {
        this.id = id;
        this.isbn = isbn;
        this.patronId = patronId;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getPatronId() { return patronId; }
    public Instant getCreatedAt() { return createdAt; }
    public ReservationStatus getStatus() { return status; }
    public String getHeldCopyId() { return heldCopyId; }

    public boolean isOpen() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.READY;
    }

    public void markReady(String copyId) {
        this.status = ReservationStatus.READY;
        this.heldCopyId = copyId;
    }

    public void backToPending() {
        this.status = ReservationStatus.PENDING;
        this.heldCopyId = null;
    }

    public void markFulfilled() { this.status = ReservationStatus.FULFILLED; }

    public void cancel() { this.status = ReservationStatus.CANCELLED; }

    @Override
    public String toString() {
        return "Reservation{" + id + ", isbn=" + isbn + ", patron=" + patronId + ", " + status + "}";
    }
}
