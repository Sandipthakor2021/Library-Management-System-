package com.library.event;

import com.library.domain.Book;
import com.library.domain.BookCopy;
import com.library.domain.LoanRecord;
import com.library.domain.Reservation;

/** Domain events published by services; listeners react without the publisher knowing about them. */
public sealed interface LibraryEvent {
    record BookCheckedOut(LoanRecord loan, BookCopy copy) implements LibraryEvent { }

    record BookReturned(LoanRecord loan, BookCopy copy) implements LibraryEvent { }

    /** A copy is on a shelf and free to be lent (returned, received from transfer, or newly added). */
    record CopyAvailable(BookCopy copy) implements LibraryEvent { }

    record CopyTransferred(BookCopy copy, String fromBranchId, String toBranchId) implements LibraryEvent { }

    record ReservationReady(Reservation reservation, BookCopy copy) implements LibraryEvent { }

    record BookRemoved(Book book) implements LibraryEvent { }
}
