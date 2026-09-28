package com.library.service;

import com.library.domain.BookCopy;
import com.library.domain.LoanRecord;
import com.library.domain.Patron;
import com.library.event.EventPublisher;
import com.library.event.LibraryEvent;
import com.library.exception.BusinessRuleException;
import com.library.exception.EntityNotFoundException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/** Checkout and return. Enforces loan limits, overdue blocking and hold-shelf ownership. */
public class LendingService {
    private static final Logger LOG = Logger.getLogger(LendingService.class.getName());

    private final PatronService patrons;
    private final BookService books;
    private final InventoryService inventory;
    private final BranchService branches;
    private final EventPublisher events;
    private final Clock clock;
    private final Map<String, LoanRecord> activeLoansByCopy = new HashMap<>();
    private final AtomicInteger loanSequence = new AtomicInteger();

    public LendingService(PatronService patrons, BookService books, InventoryService inventory,
                          BranchService branches, EventPublisher events, Clock clock) {
        this.patrons = patrons;
        this.books = books;
        this.inventory = inventory;
        this.branches = branches;
        this.events = events;
        this.clock = clock;
    }

    /** Borrow a copy of {@code isbn} held at {@code branchId}. */
    public synchronized LoanRecord checkOut(String patronId, String isbn, String branchId) {
        Patron patron = patrons.get(patronId);
        branches.get(branchId);
        String key = books.getBook(isbn).isbn();
        LocalDate today = LocalDate.now(clock);

        if (patron.getActiveLoans().size() >= patron.getMaxActiveLoans()) {
            throw new BusinessRuleException(patron.getName() + " has reached the limit of "
                    + patron.getMaxActiveLoans() + " active loans");
        }
        if (patron.hasOverdueLoans(today)) {
            throw new BusinessRuleException(patron.getName() + " has overdue loans and cannot borrow");
        }

        BookCopy copy = pickCopy(patron, key, branchId);
        copy.checkOut(patronId);

        LoanRecord loan = new LoanRecord(String.format("L-%05d", loanSequence.incrementAndGet()), copy.getId(), key,
                patronId, branchId, today, today.plusDays(patron.getLoanPeriodDays()));
        patron.addLoan(loan);
        activeLoansByCopy.put(copy.getId(), loan);
        LOG.info("CHECKOUT " + loan.getId() + ": " + patron.getName() + " borrowed " + key + " (copy "
                + copy.getId() + ") from " + branchId + ", due " + loan.getDueDate());
        events.publish(new LibraryEvent.BookCheckedOut(loan, copy));
        return loan;
    }

    /** Return a copy at {@code returnBranchId}; the copy then belongs to that branch. */
    public synchronized LoanRecord returnBook(String copyId, String returnBranchId) {
        branches.get(returnBranchId);
        LoanRecord loan = activeLoansByCopy.get(copyId);
        if (loan == null) {
            throw new BusinessRuleException("Copy " + copyId + " is not currently on loan");
        }
        BookCopy copy = inventory.getCopy(copyId);
        copy.markReturned(returnBranchId);
        loan.markReturned(LocalDate.now(clock));
        activeLoansByCopy.remove(copyId);

        LOG.info("RETURN " + loan.getId() + ": copy " + copyId + " returned to " + returnBranchId
                + (loan.getReturnDate().isAfter(loan.getDueDate()) ? " (LATE)" : ""));
        events.publish(new LibraryEvent.BookReturned(loan, copy));
        events.publish(new LibraryEvent.CopyAvailable(copy));
        return loan;
    }

    public synchronized List<LoanRecord> getActiveLoans() {
        return activeLoansByCopy.values().stream().sorted(Comparator.comparing(LoanRecord::getId)).toList();
    }

    public synchronized List<LoanRecord> getOverdueLoans() {
        LocalDate today = LocalDate.now(clock);
        return activeLoansByCopy.values().stream().filter(l -> l.isOverdue(today))
                .sorted(Comparator.comparing(LoanRecord::getDueDate)).toList();
    }

    /** Prefer the copy on hold for this patron, otherwise any free copy at the branch. */
    private BookCopy pickCopy(Patron patron, String isbn, String branchId) {
        List<BookCopy> atBranch = inventory.copiesOf(isbn).stream()
                .filter(c -> c.getBranchId().equals(branchId)).toList();
        return atBranch.stream().filter(c -> patron.getId().equals(c.getHeldForPatronId())).findFirst()
                .or(() -> atBranch.stream().filter(BookCopy::isAvailable).findFirst())
                .orElseThrow(() -> new BusinessRuleException("No available copy of " + isbn + " at branch "
                        + branchId + "; consider reserving it"));
    }
}
