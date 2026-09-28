package com.library;

import com.library.domain.Book;
import com.library.domain.Branch;
import com.library.domain.Patron;
import com.library.domain.PatronFactory;
import com.library.event.EventPublisher;
import com.library.notification.NotificationService;
import com.library.notification.ReservationNotificationListener;
import com.library.recommendation.CollaborativeFilteringStrategy;
import com.library.recommendation.GenreAffinityStrategy;
import com.library.recommendation.RecommendationService;
import com.library.repository.InMemoryCopyRepository;
import com.library.repository.InMemoryRepository;
import com.library.repository.Repository;
import com.library.search.BookSearchService;
import com.library.service.BookService;
import com.library.service.BranchService;
import com.library.service.InventoryService;
import com.library.service.LendingService;
import com.library.service.PatronService;
import com.library.service.ReservationService;
import com.library.service.TransferService;

import java.time.Clock;
import java.util.List;

/**
 * Composition root: the only place that knows concrete classes. Everything is constructor-injected, so tests
 * can swap the clock, the notification channel or the repositories.
 */
public class LibrarySystem {
    public final EventPublisher events = new EventPublisher();
    public final BranchService branches;
    public final BookService books;
    public final BookSearchService search;
    public final PatronService patrons;
    public final InventoryService inventory;
    public final LendingService lending;
    public final ReservationService reservations;
    public final TransferService transfers;
    public final RecommendationService recommendations;

    public LibrarySystem(Clock clock, NotificationService notifications) {
        Repository<Book, String> bookRepo = new InMemoryRepository<>(Book::isbn);
        Repository<Patron, String> patronRepo = new InMemoryRepository<>(Patron::getId);
        Repository<Branch, String> branchRepo = new InMemoryRepository<>(Branch::id);
        InMemoryCopyRepository copyRepo = new InMemoryCopyRepository();

        branches = new BranchService(branchRepo);
        books = new BookService(bookRepo, copyRepo, events);
        search = new BookSearchService(bookRepo);
        patrons = new PatronService(patronRepo, new PatronFactory());
        inventory = new InventoryService(bookRepo, copyRepo, branches, events);
        lending = new LendingService(patrons, books, inventory, branches, events, clock);
        reservations = new ReservationService(patrons, books, inventory, events, clock);
        transfers = new TransferService(inventory, branches, events);
        recommendations = new RecommendationService(patronRepo, List.of(
                new CollaborativeFilteringStrategy(bookRepo, patronRepo),
                new GenreAffinityStrategy(bookRepo)));

        events.subscribe(reservations);
        events.subscribe(new ReservationNotificationListener(patronRepo, bookRepo, notifications));
    }
}
