package com.library;

import com.library.domain.Book;
import com.library.domain.BookCopy;
import com.library.domain.CopyStatus;
import com.library.domain.LoanRecord;
import com.library.domain.Patron;
import com.library.domain.PatronType;
import com.library.domain.Reservation;
import com.library.domain.ReservationStatus;
import com.library.exception.BusinessRuleException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.EntityNotFoundException;
import com.library.notification.NotificationService;
import com.library.util.LogConfig;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Dependency-free test suite (no JUnit needed so the project builds offline).
 * Run: {@code java -cp out:out-test com.library.LibrarySystemTest} - exits non-zero on failure.
 */
public class LibrarySystemTest {
    static final String EFFECTIVE = "978-0-13-468599-1";
    static final String CLEAN = "978-0-13-235088-4";
    static final String DUNE = "978-0-44-101359-3";
    static final String FOUNDATION = "978-0-55-338256-3";

    static int passed = 0;
    static final List<String> failures = new ArrayList<>();

    // ---- tiny test harness ----
    interface TestBody { void run() throws Exception; }

    static void test(String name, TestBody body) {
        try {
            body.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (Throwable t) {
            failures.add(name + " -> " + t);
            System.out.println("FAIL  " + name + " -> " + t);
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void eq(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) throw new AssertionError(message + ": expected " + expected + " but was " + actual);
    }

    static void throwsA(Class<? extends Exception> type, Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            if (type.isInstance(e)) return;
            throw new AssertionError("Expected " + type.getSimpleName() + " but got " + e);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " but nothing was thrown");
    }

    /** Clock the tests can move forward to simulate overdue loans. */
    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T10:00:00Z");
        void advanceDays(long days) { now = now.plus(Duration.ofDays(days)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    static class RecordingNotifications implements NotificationService {
        final List<String> messages = new ArrayList<>();
        @Override public void notify(Patron patron, String message) { messages.add(patron.getId() + ": " + message); }
    }

    static class Fixture {
        final MutableClock clock = new MutableClock();
        final RecordingNotifications notifications = new RecordingNotifications();
        final LibrarySystem lib = new LibrarySystem(clock, notifications);
        final Patron alice, bob, carol;

        Fixture() {
            lib.branches.addBranch("MAIN", "Main", "1 Main St");
            lib.branches.addBranch("EAST", "East", "2 East St");
            lib.books.addBook(book(EFFECTIVE, "Effective Java", "Joshua Bloch", "Programming"));
            lib.books.addBook(book(CLEAN, "Clean Code", "Robert C. Martin", "Programming"));
            lib.books.addBook(book(DUNE, "Dune", "Frank Herbert", "Science Fiction"));
            lib.books.addBook(book(FOUNDATION, "Foundation", "Isaac Asimov", "Science Fiction"));
            alice = lib.patrons.register(PatronType.STANDARD, "Alice", "alice@example.com", "1");
            bob = lib.patrons.register(PatronType.STUDENT, "Bob", "bob@example.com", "2");
            carol = lib.patrons.register(PatronType.PREMIUM, "Carol", "carol@example.com", "3");
        }
    }

    static Book book(String isbn, String title, String author, String genre) {
        return Book.builder().isbn(isbn).title(title).author(author).publicationYear(2000).genre(genre).build();
    }

    public static void main(String[] args) {
        LogConfig.init(Level.WARNING); // keep test output readable

        // ---------------- Book management ----------------
        test("add book rejects duplicate ISBN (hyphens ignored)", () -> {
            Fixture f = new Fixture();
            throwsA(DuplicateEntityException.class, () -> f.lib.books.addBook(book("9780134685991", "Other", "X", "Y")));
        });
        test("book validation rejects bad ISBN, blank title, future year", () -> {
            throwsA(IllegalArgumentException.class, () -> book("123", "T", "A", "G"));
            throwsA(IllegalArgumentException.class, () -> book(EFFECTIVE, " ", "A", "G"));
            throwsA(IllegalArgumentException.class,
                    () -> Book.builder().isbn(EFFECTIVE).title("T").author("A").publicationYear(3000).build());
        });
        test("update book changes catalogue data; unknown ISBN fails", () -> {
            Fixture f = new Fixture();
            Book old = f.lib.books.getBook(EFFECTIVE);
            f.lib.books.updateBook(old.toBuilder().title("Effective Java 3rd Ed").build());
            eq("Effective Java 3rd Ed", f.lib.books.getBook(EFFECTIVE).title(), "title");
            throwsA(EntityNotFoundException.class,
                    () -> f.lib.books.updateBook(book("978-1-11-111111-6", "Ghost", "Nobody", "None")));
        });
        test("remove book deletes its copies; blocked while a copy is borrowed", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(EFFECTIVE, "MAIN", 2);
            f.lib.inventory.addCopies(CLEAN, "MAIN", 1);
            f.lib.lending.checkOut(f.alice.getId(), CLEAN, "MAIN");
            throwsA(BusinessRuleException.class, () -> f.lib.books.removeBook(CLEAN));
            f.lib.books.removeBook(EFFECTIVE);
            check(f.lib.search.byIsbn(EFFECTIVE).isEmpty(), "book gone");
            check(f.lib.inventory.copiesOf(EFFECTIVE).isEmpty(), "copies gone");
        });

        // ---------------- Search ----------------
        test("search by title, author, ISBN", () -> {
            Fixture f = new Fixture();
            eq(1, f.lib.search.byTitle("effective").size(), "title partial, case-insensitive");
            eq(1, f.lib.search.byAuthor("ASIMOV").size(), "author");
            eq(3, f.lib.search.byTitle("e").size(), "multiple matches (Effective, Clean, Dune)");
            eq("Dune", f.lib.search.byIsbn("9780441013593").get(0).title(), "isbn");
            check(f.lib.search.byTitle("").isEmpty(), "blank query");
            check(f.lib.search.byTitle("nothing like this").isEmpty(), "no match");
        });

        // ---------------- Patrons ----------------
        test("patron types apply different lending policies", () -> {
            Fixture f = new Fixture();
            eq(3, f.bob.getMaxActiveLoans(), "student max");
            eq(5, f.alice.getMaxActiveLoans(), "standard max");
            eq(10, f.carol.getMaxActiveLoans(), "premium max");
            eq(14, f.bob.getLoanPeriodDays(), "student days");
        });
        test("update patron; duplicate email rejected", () -> {
            Fixture f = new Fixture();
            f.lib.patrons.updateContact(f.alice.getId(), "Alice K", "alice.k@example.com", "999");
            eq("Alice K", f.lib.patrons.get(f.alice.getId()).getName(), "name");
            throwsA(DuplicateEntityException.class,
                    () -> f.lib.patrons.updateContact(f.alice.getId(), "Alice", "bob@example.com", "1"));
            throwsA(DuplicateEntityException.class,
                    () -> f.lib.patrons.register(PatronType.STANDARD, "Bob2", "BOB@example.com", "0"));
            throwsA(IllegalArgumentException.class,
                    () -> f.lib.patrons.register(PatronType.STANDARD, "Bad", "not-an-email", "0"));
        });

        // ---------------- Lending + inventory ----------------
        test("checkout and return update inventory and history", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(DUNE, "MAIN", 2);
            eq(2, f.lib.inventory.availableCount(DUNE), "available before");
            LoanRecord loan = f.lib.lending.checkOut(f.alice.getId(), DUNE, "MAIN");
            eq(1, f.lib.inventory.availableCount(DUNE), "available after checkout");
            eq(1, f.lib.inventory.borrowedCopies().size(), "borrowed");
            eq(21, (int) java.time.temporal.ChronoUnit.DAYS.between(loan.getCheckoutDate(), loan.getDueDate()), "loan period");
            f.lib.lending.returnBook(loan.getCopyId(), "MAIN");
            eq(2, f.lib.inventory.availableCount(DUNE), "available after return");
            eq(1, f.lib.patrons.getBorrowingHistory(f.alice.getId()).size(), "history size");
            check(!f.lib.patrons.getBorrowingHistory(f.alice.getId()).get(0).isActive(), "history entry closed");
            check(f.alice.getActiveLoans().isEmpty(), "no active loans");
        });
        test("checkout fails when no copy at branch; returning an unborrowed copy fails", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(DUNE, "EAST", 1);
            throwsA(BusinessRuleException.class, () -> f.lib.lending.checkOut(f.alice.getId(), DUNE, "MAIN"));
            BookCopy copy = f.lib.inventory.copiesOf(DUNE).get(0);
            throwsA(BusinessRuleException.class, () -> f.lib.lending.returnBook(copy.getId(), "EAST"));
            throwsA(EntityNotFoundException.class, () -> f.lib.lending.checkOut("P-9999", DUNE, "EAST"));
        });
        test("loan limit enforced per patron type", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(DUNE, "MAIN", 1);
            f.lib.inventory.addCopies(CLEAN, "MAIN", 1);
            f.lib.inventory.addCopies(EFFECTIVE, "MAIN", 1);
            f.lib.inventory.addCopies(FOUNDATION, "MAIN", 1);
            f.lib.lending.checkOut(f.bob.getId(), DUNE, "MAIN");
            f.lib.lending.checkOut(f.bob.getId(), CLEAN, "MAIN");
            f.lib.lending.checkOut(f.bob.getId(), EFFECTIVE, "MAIN");
            throwsA(BusinessRuleException.class, () -> f.lib.lending.checkOut(f.bob.getId(), FOUNDATION, "MAIN"));
        });
        test("overdue patron cannot borrow until the book is returned", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(DUNE, "MAIN", 1);
            f.lib.inventory.addCopies(CLEAN, "MAIN", 1);
            LoanRecord loan = f.lib.lending.checkOut(f.bob.getId(), DUNE, "MAIN"); // 14 days
            f.clock.advanceDays(15);
            eq(1, f.lib.lending.getOverdueLoans().size(), "overdue list");
            throwsA(BusinessRuleException.class, () -> f.lib.lending.checkOut(f.bob.getId(), CLEAN, "MAIN"));
            f.lib.lending.returnBook(loan.getCopyId(), "MAIN");
            f.lib.lending.checkOut(f.bob.getId(), CLEAN, "MAIN");
        });
        test("return at another branch moves the copy there", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(DUNE, "MAIN", 1);
            LoanRecord loan = f.lib.lending.checkOut(f.alice.getId(), DUNE, "MAIN");
            f.lib.lending.returnBook(loan.getCopyId(), "EAST");
            eq("EAST", f.lib.inventory.getCopy(loan.getCopyId()).getBranchId(), "branch");
        });
        test("inventory report groups by branch and status", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(DUNE, "MAIN", 2);
            f.lib.inventory.addCopies(CLEAN, "EAST", 1);
            f.lib.lending.checkOut(f.alice.getId(), DUNE, "MAIN");
            var report = f.lib.inventory.report();
            eq(1L, report.get("MAIN").get(CopyStatus.AVAILABLE), "main available");
            eq(1L, report.get("MAIN").get(CopyStatus.BORROWED), "main borrowed");
            eq(1L, report.get("EAST").get(CopyStatus.AVAILABLE), "east available");
        });

        // ---------------- Reservations ----------------
        test("reservation: hold on return, notify, only holder can borrow, then fulfilled", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(EFFECTIVE, "MAIN", 1);
            LoanRecord loan = f.lib.lending.checkOut(f.alice.getId(), EFFECTIVE, "MAIN");
            Reservation r = f.lib.reservations.reserve(f.bob.getId(), EFFECTIVE);
            eq(ReservationStatus.PENDING, r.getStatus(), "pending");
            check(f.notifications.messages.isEmpty(), "nothing yet");

            f.lib.lending.returnBook(loan.getCopyId(), "MAIN");
            eq(ReservationStatus.READY, r.getStatus(), "ready");
            eq(CopyStatus.RESERVED, f.lib.inventory.getCopy(loan.getCopyId()).getStatus(), "copy on hold");
            eq(1, f.notifications.messages.size(), "one notification");
            check(f.notifications.messages.get(0).startsWith(f.bob.getId()), "bob notified");

            throwsA(BusinessRuleException.class, () -> f.lib.lending.checkOut(f.carol.getId(), EFFECTIVE, "MAIN"));
            f.lib.lending.checkOut(f.bob.getId(), EFFECTIVE, "MAIN");
            eq(ReservationStatus.FULFILLED, r.getStatus(), "fulfilled");
            check(f.lib.reservations.getQueue(EFFECTIVE).isEmpty(), "queue empty");
        });
        test("reservation queue is FIFO and cancelling a ready hold passes the copy on", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(EFFECTIVE, "MAIN", 1);
            LoanRecord loan = f.lib.lending.checkOut(f.alice.getId(), EFFECTIVE, "MAIN");
            Reservation rBob = f.lib.reservations.reserve(f.bob.getId(), EFFECTIVE);
            Reservation rCarol = f.lib.reservations.reserve(f.carol.getId(), EFFECTIVE);
            f.lib.lending.returnBook(loan.getCopyId(), "MAIN");
            eq(ReservationStatus.READY, rBob.getStatus(), "bob first");
            eq(ReservationStatus.PENDING, rCarol.getStatus(), "carol waits");

            f.lib.reservations.cancel(rBob.getId());
            eq(ReservationStatus.READY, rCarol.getStatus(), "carol next");
            eq(2, f.notifications.messages.size(), "bob then carol notified");
        });
        test("cannot reserve an available title or reserve twice", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(EFFECTIVE, "MAIN", 1);
            throwsA(BusinessRuleException.class, () -> f.lib.reservations.reserve(f.bob.getId(), EFFECTIVE));
            f.lib.lending.checkOut(f.alice.getId(), EFFECTIVE, "MAIN");
            f.lib.reservations.reserve(f.bob.getId(), EFFECTIVE);
            throwsA(BusinessRuleException.class, () -> f.lib.reservations.reserve(f.bob.getId(), EFFECTIVE));
            throwsA(BusinessRuleException.class, () -> f.lib.reservations.reserve(f.alice.getId(), EFFECTIVE));
        });

        // ---------------- Multi-branch transfer ----------------
        test("transfer: dispatch puts copy in transit, receive makes it available at destination", () -> {
            Fixture f = new Fixture();
            BookCopy copy = f.lib.inventory.addCopies(DUNE, "EAST", 1).get(0);
            f.lib.transfers.dispatch(copy.getId(), "MAIN");
            eq(CopyStatus.IN_TRANSIT, copy.getStatus(), "in transit");
            throwsA(BusinessRuleException.class, () -> f.lib.lending.checkOut(f.alice.getId(), DUNE, "EAST"));
            f.lib.transfers.receive(copy.getId());
            eq("MAIN", copy.getBranchId(), "moved");
            f.lib.lending.checkOut(f.alice.getId(), DUNE, "MAIN");
        });
        test("transfer rules: same branch and borrowed copies rejected", () -> {
            Fixture f = new Fixture();
            BookCopy copy = f.lib.inventory.addCopies(DUNE, "MAIN", 1).get(0);
            throwsA(BusinessRuleException.class, () -> f.lib.transfers.dispatch(copy.getId(), "MAIN"));
            f.lib.lending.checkOut(f.alice.getId(), DUNE, "MAIN");
            throwsA(BusinessRuleException.class, () -> f.lib.transfers.dispatch(copy.getId(), "EAST"));
            throwsA(EntityNotFoundException.class, () -> f.lib.transfers.dispatch(copy.getId(), "NOWHERE"));
        });
        test("transferred copy is held for a waiting reservation", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(DUNE, "MAIN", 1);
            BookCopy east = f.lib.inventory.addCopies(DUNE, "EAST", 1).get(0);
            f.lib.lending.checkOut(f.alice.getId(), DUNE, "MAIN");
            f.lib.lending.checkOut(f.carol.getId(), DUNE, "EAST");
            Reservation r = f.lib.reservations.reserve(f.bob.getId(), DUNE);
            f.lib.lending.returnBook(east.getId(), "EAST");
            eq(ReservationStatus.READY, r.getStatus(), "held at EAST");
            eq("EAST", f.lib.inventory.getCopy(east.getId()).getBranchId(), "branch");
        });

        // ---------------- Recommendations ----------------
        test("recommendations: genre affinity and preferences, excluding already-read books", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(DUNE, "MAIN", 1);
            f.lib.inventory.addCopies(EFFECTIVE, "MAIN", 1);
            f.lib.patrons.updatePreferences(f.alice.getId(), Set.of("Science Fiction"));
            List<Book> recs = f.lib.recommendations.recommend(f.alice.getId(), 5);
            List<String> titles = recs.stream().map(Book::title).toList();
            check(titles.containsAll(List.of("Dune", "Foundation")), "sci-fi suggested: " + titles);
            f.lib.lending.checkOut(f.alice.getId(), DUNE, "MAIN");
            titles = f.lib.recommendations.recommend(f.alice.getId(), 5).stream().map(Book::title).toList();
            check(!titles.contains("Dune"), "already borrowed excluded: " + titles);
            eq("Foundation", titles.get(0), "closest match first");
        });
        test("recommendations: collaborative filtering surfaces what similar patrons borrowed", () -> {
            Fixture f = new Fixture();
            f.lib.inventory.addCopies(DUNE, "MAIN", 1);
            f.lib.inventory.addCopies(CLEAN, "MAIN", 1);
            f.lib.inventory.addCopies(EFFECTIVE, "MAIN", 1);
            // Alice and Bob both read Dune; Bob also read Clean Code (different genre than anything Alice likes).
            for (Patron p : List.of(f.alice, f.bob)) {
                LoanRecord l = f.lib.lending.checkOut(p.getId(), DUNE, "MAIN");
                f.lib.lending.returnBook(l.getCopyId(), "MAIN");
            }
            LoanRecord l = f.lib.lending.checkOut(f.bob.getId(), CLEAN, "MAIN");
            f.lib.lending.returnBook(l.getCopyId(), "MAIN");

            List<String> titles = f.lib.recommendations.recommend(f.alice.getId(), 1).stream().map(Book::title).toList();
            eq(List.of("Clean Code"), titles, "top collaborative pick");
        });
        test("recommendations: new patron with no history or preferences gets an empty list", () -> {
            Fixture f = new Fixture();
            check(f.lib.recommendations.recommend(f.carol.getId(), 5).isEmpty(), "empty");
        });

        System.out.println("\n" + passed + " passed, " + failures.size() + " failed");
        failures.forEach(x -> System.out.println("  FAILED: " + x));
        System.exit(failures.isEmpty() ? 0 : 1);
    }
}
