package com.library;

import com.library.domain.Book;
import com.library.domain.BookCopy;
import com.library.domain.LoanRecord;
import com.library.domain.Patron;
import com.library.domain.PatronType;
import com.library.domain.Reservation;
import com.library.exception.LibraryException;
import com.library.notification.ConsoleNotificationService;
import com.library.util.LogConfig;

import java.time.Clock;
import java.util.Set;
import java.util.logging.Level;

/** End-to-end demo. Run with: {@code java -cp out com.library.Main [debug]}. */
public class Main {

    public static void main(String[] args) {
        LogConfig.init(args.length > 0 && args[0].equals("debug") ? Level.FINE : Level.INFO);
        LibrarySystem lib = new LibrarySystem(Clock.systemDefaultZone(), new ConsoleNotificationService());

        section("1. Branches, catalogue and inventory");
        lib.branches.addBranch("MAIN", "Central Library", "1 Library Square");
        lib.branches.addBranch("EAST", "East Branch", "22 East Road");

        lib.books.addBook(Book.builder().isbn("978-0-13-468599-1").title("Effective Java").author("Joshua Bloch")
                .publicationYear(2018).genre("Programming").build());
        lib.books.addBook(Book.builder().isbn("978-0-13-235088-4").title("Clean Code").author("Robert C. Martin")
                .publicationYear(2008).genre("Programming").build());
        lib.books.addBook(Book.builder().isbn("978-0-20-163361-0").title("Design Patterns").author("Erich Gamma")
                .publicationYear(1994).genre("Programming").build());
        lib.books.addBook(Book.builder().isbn("978-0-44-101359-3").title("Dune").author("Frank Herbert")
                .publicationYear(1965).genre("Science Fiction").build());
        lib.books.addBook(Book.builder().isbn("978-0-55-338256-3").title("Foundation").author("Isaac Asimov")
                .publicationYear(1951).genre("Science Fiction").build());
        lib.books.addBook(Book.builder().isbn("978-0-14-143951-8").title("Pride and Prejudice").author("Jane Austen")
                .publicationYear(1813).genre("Classic").build());

        lib.inventory.addCopies("978-0-13-468599-1", "MAIN", 1);   // only one copy -> good for reservations
        lib.inventory.addCopies("978-0-13-235088-4", "MAIN", 2);
        lib.inventory.addCopies("978-0-20-163361-0", "MAIN", 1);
        lib.inventory.addCopies("978-0-44-101359-3", "MAIN", 1);
        lib.inventory.addCopies("978-0-44-101359-3", "EAST", 1);
        lib.inventory.addCopies("978-0-55-338256-3", "EAST", 2);
        lib.inventory.addCopies("978-0-14-143951-8", "EAST", 1);

        section("2. Update a book and search");
        Book clean = lib.books.getBook("9780132350884");
        lib.books.updateBook(clean.toBuilder().title("Clean Code: A Handbook of Agile Software Craftsmanship").build());
        System.out.println("By title 'clean'   : " + lib.search.byTitle("clean"));
        System.out.println("By author 'asimov' : " + lib.search.byAuthor("asimov"));
        System.out.println("By ISBN            : " + lib.search.byIsbn("978-0-44-101359-3"));

        section("3. Patrons");
        Patron alice = lib.patrons.register(PatronType.STANDARD, "Alice Kumar", "alice@example.com", "555-0101");
        Patron bob = lib.patrons.register(PatronType.STUDENT, "Bob Singh", "bob@example.com", "555-0102");
        Patron carol = lib.patrons.register(PatronType.PREMIUM, "Carol Diaz", "carol@example.com", "555-0103");
        lib.patrons.updatePreferences(carol.getId(), Set.of("Science Fiction"));
        lib.patrons.updateContact(bob.getId(), "Bob Singh", "bob.singh@example.com", "555-0199");

        section("4. Checkout / return and business rules");
        LoanRecord aliceEffective = lib.lending.checkOut(alice.getId(), "978-0-13-468599-1", "MAIN");
        LoanRecord aliceClean = lib.lending.checkOut(alice.getId(), "978-0-13-235088-4", "MAIN");
        LoanRecord bobDune = lib.lending.checkOut(bob.getId(), "978-0-44-101359-3", "MAIN");
        LoanRecord bobDesign = lib.lending.checkOut(bob.getId(), "978-0-20-163361-0", "MAIN");
        tryIt("Bob borrows Design Patterns again (no copy left)",
                () -> lib.lending.checkOut(bob.getId(), "978-0-20-163361-0", "MAIN"));
        System.out.println("Borrowed copies: " + lib.inventory.borrowedCopies().size());
        System.out.println("Inventory report: " + lib.inventory.report());

        section("5. Reservation + notification (Observer)");
        Reservation reservation = lib.reservations.reserve(carol.getId(), "978-0-13-468599-1");
        tryIt("Carol reserves again", () -> lib.reservations.reserve(carol.getId(), "978-0-13-468599-1"));
        lib.lending.returnBook(aliceEffective.getCopyId(), "MAIN");   // triggers hold + notification
        tryIt("Bob tries to take Carol's held copy", () -> lib.lending.checkOut(bob.getId(), "978-0-13-468599-1", "MAIN"));
        lib.lending.checkOut(carol.getId(), "978-0-13-468599-1", "MAIN");
        System.out.println("Reservation status: " + reservation.getStatus());

        section("6. Multi-branch transfer");
        BookCopy eastDune = lib.inventory.copiesOf("978-0-44-101359-3").stream()
                .filter(c -> c.getBranchId().equals("EAST")).findFirst().orElseThrow();
        lib.transfers.dispatch(eastDune.getId(), "MAIN");
        System.out.println("In transit: " + eastDune);
        lib.transfers.receive(eastDune.getId());
        System.out.println("After receive: " + eastDune);
        lib.lending.returnBook(bobDune.getCopyId(), "EAST");   // returned at a different branch than borrowed
        System.out.println("Title summary: " + lib.inventory.titleSummary());

        section("7. Borrowing history and recommendations");
        lib.lending.returnBook(aliceClean.getCopyId(), "MAIN");
        lib.lending.returnBook(bobDesign.getCopyId(), "MAIN");
        lib.lending.checkOut(alice.getId(), "978-0-20-163361-0", "MAIN");
        lib.patrons.getBorrowingHistory(alice.getId()).forEach(l -> System.out.println("  " + l));
        System.out.println("Recommendations for Alice: " + titles(lib.recommendations.recommend(alice.getId(), 3)));
        System.out.println("Recommendations for Bob  : " + titles(lib.recommendations.recommend(bob.getId(), 3)));
        System.out.println("Recommendations for Carol: " + titles(lib.recommendations.recommend(carol.getId(), 3)));
    }

    private static java.util.List<String> titles(java.util.List<Book> books) {
        return books.stream().map(Book::title).toList();
    }

    private static void section(String title) {
        System.out.println("\n===== " + title + " =====");
    }

    private static void tryIt(String description, Runnable action) {
        try {
            action.run();
            System.out.println("UNEXPECTED SUCCESS: " + description);
        } catch (LibraryException e) {
            System.out.println("Rejected as expected [" + description + "]: " + e.getMessage());
        }
    }
}
