package com.library.domain;

import com.library.exception.BusinessRuleException;

/**
 * A physical copy of a {@link Book} that lives in one branch. The copy owns its state machine
 * (AVAILABLE, BORROWED, RESERVED, IN_TRANSIT) so illegal transitions cannot be performed from outside.
 */
public class BookCopy {
    private final String id;
    private final String isbn;
    private String branchId;
    private CopyStatus status = CopyStatus.AVAILABLE;
    private String heldForPatronId;
    private String transitDestination;

    public BookCopy(String id, String isbn, String branchId) {
        this.id = id;
        this.isbn = isbn;
        this.branchId = branchId;
    }

    public String getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getBranchId() { return branchId; }
    public CopyStatus getStatus() { return status; }
    public String getHeldForPatronId() { return heldForPatronId; }
    public String getTransitDestination() { return transitDestination; }

    public boolean isAvailable() {
        return status == CopyStatus.AVAILABLE;
    }

    /** Allowed from AVAILABLE, or from RESERVED when the borrower is the patron the copy is held for. */
    public void checkOut(String patronId) {
        boolean heldForThisPatron = status == CopyStatus.RESERVED && patronId.equals(heldForPatronId);
        if (status != CopyStatus.AVAILABLE && !heldForThisPatron) {
            throw new BusinessRuleException("Copy " + id + " cannot be checked out (status " + status + ")");
        }
        status = CopyStatus.BORROWED;
        heldForPatronId = null;
    }

    public void markReturned(String returnBranchId) {
        require(CopyStatus.BORROWED, "returned");
        branchId = returnBranchId;
        status = CopyStatus.AVAILABLE;
    }

    public void placeHold(String patronId) {
        require(CopyStatus.AVAILABLE, "put on hold");
        status = CopyStatus.RESERVED;
        heldForPatronId = patronId;
    }

    public void releaseHold() {
        require(CopyStatus.RESERVED, "released from hold");
        status = CopyStatus.AVAILABLE;
        heldForPatronId = null;
    }

    public void startTransit(String destinationBranchId) {
        require(CopyStatus.AVAILABLE, "transferred");
        status = CopyStatus.IN_TRANSIT;
        transitDestination = destinationBranchId;
    }

    public void completeTransit() {
        require(CopyStatus.IN_TRANSIT, "received");
        branchId = transitDestination;
        transitDestination = null;
        status = CopyStatus.AVAILABLE;
    }

    private void require(CopyStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleException(
                    "Copy " + id + " cannot be " + action + " (status " + status + ", expected " + expected + ")");
        }
    }

    @Override
    public String toString() {
        return "BookCopy{" + id + ", isbn=" + isbn + ", branch=" + branchId + ", " + status + "}";
    }
}
