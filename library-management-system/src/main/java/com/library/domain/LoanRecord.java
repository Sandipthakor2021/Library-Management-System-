package com.library.domain;

import java.time.LocalDate;

/** One checkout of one copy by one patron. Entries form the patron's borrowing history. */
public class LoanRecord {
    private final String id;
    private final String copyId;
    private final String isbn;
    private final String patronId;
    private final String branchId;
    private final LocalDate checkoutDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;

    public LoanRecord(String id, String copyId, String isbn, String patronId, String branchId,
                      LocalDate checkoutDate, LocalDate dueDate) {
        this.id = id;
        this.copyId = copyId;
        this.isbn = isbn;
        this.patronId = patronId;
        this.branchId = branchId;
        this.checkoutDate = checkoutDate;
        this.dueDate = dueDate;
    }

    public String getId() { return id; }
    public String getCopyId() { return copyId; }
    public String getIsbn() { return isbn; }
    public String getPatronId() { return patronId; }
    public String getBranchId() { return branchId; }
    public LocalDate getCheckoutDate() { return checkoutDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }

    public boolean isActive() {
        return returnDate == null;
    }

    public boolean isOverdue(LocalDate today) {
        return isActive() && today.isAfter(dueDate);
    }

    public void markReturned(LocalDate date) {
        this.returnDate = date;
    }

    @Override
    public String toString() {
        return "Loan{" + id + ", copy=" + copyId + ", isbn=" + isbn + ", patron=" + patronId
                + ", out=" + checkoutDate + ", due=" + dueDate
                + (returnDate != null ? ", returned=" + returnDate : "") + "}";
    }
}
