package com.library.service;

import com.library.domain.Book;
import com.library.domain.BookCopy;
import com.library.domain.CopyStatus;
import com.library.event.EventPublisher;
import com.library.event.LibraryEvent;
import com.library.exception.BusinessRuleException;
import com.library.exception.EntityNotFoundException;
import com.library.repository.CopyRepository;
import com.library.repository.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** Tracks physical copies: where they are and whether they are available or borrowed. */
public class InventoryService {
    private static final Logger LOG = Logger.getLogger(InventoryService.class.getName());

    private final Repository<Book, String> books;
    private final CopyRepository copies;
    private final BranchService branches;
    private final EventPublisher events;
    private final AtomicInteger copySequence = new AtomicInteger();

    public InventoryService(Repository<Book, String> books, CopyRepository copies, BranchService branches,
                            EventPublisher events) {
        this.books = books;
        this.copies = copies;
        this.branches = branches;
        this.events = events;
    }

    public List<BookCopy> addCopies(String isbn, String branchId, int count) {
        String key = Book.normalizeIsbn(isbn);
        if (!books.existsById(key)) throw new EntityNotFoundException("Book", key);
        branches.get(branchId);
        if (count <= 0) throw new IllegalArgumentException("count must be positive");

        List<BookCopy> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BookCopy copy = new BookCopy(String.format("C-%04d", copySequence.incrementAndGet()), key, branchId);
            copies.save(copy);
            created.add(copy);
        }
        LOG.info("Added " + count + " copy/copies of " + key + " to branch " + branchId);
        created.forEach(c -> events.publish(new LibraryEvent.CopyAvailable(c)));
        return created;
    }

    public void removeCopy(String copyId) {
        BookCopy copy = getCopy(copyId);
        if (!copy.isAvailable()) {
            throw new BusinessRuleException("Copy " + copyId + " is " + copy.getStatus() + " and cannot be removed");
        }
        copies.deleteById(copyId);
        LOG.info("Copy removed: " + copyId);
    }

    public BookCopy getCopy(String copyId) {
        return copies.findById(copyId).orElseThrow(() -> new EntityNotFoundException("Copy", copyId));
    }

    public List<BookCopy> copiesOf(String isbn) {
        return copies.findByIsbn(Book.normalizeIsbn(isbn));
    }

    public List<BookCopy> availableCopies(String isbn) {
        return copiesOf(isbn).stream().filter(BookCopy::isAvailable).toList();
    }

    public int availableCount(String isbn) {
        return availableCopies(isbn).size();
    }

    public List<BookCopy> borrowedCopies() {
        return copies.findAll().stream().filter(c -> c.getStatus() == CopyStatus.BORROWED)
                .sorted(Comparator.comparing(BookCopy::getId)).toList();
    }

    /** Copy counts per branch and status, e.g. {MAIN={AVAILABLE=3, BORROWED=1}}. */
    public Map<String, Map<CopyStatus, Long>> report() {
        Map<String, Map<CopyStatus, Long>> report = new TreeMap<>();
        for (BookCopy c : copies.findAll()) {
            report.computeIfAbsent(c.getBranchId(), k -> new EnumMap<>(CopyStatus.class))
                    .merge(c.getStatus(), 1L, Long::sum);
        }
        return report;
    }

    /** Per title: total / available / borrowed counts across all branches. */
    public Map<String, String> titleSummary() {
        return copies.findAll().stream().collect(Collectors.groupingBy(BookCopy::getIsbn, TreeMap::new,
                Collectors.collectingAndThen(Collectors.toList(), list -> {
                    long available = list.stream().filter(BookCopy::isAvailable).count();
                    long borrowed = list.stream().filter(c -> c.getStatus() == CopyStatus.BORROWED).count();
                    return "total=" + list.size() + ", available=" + available + ", borrowed=" + borrowed;
                })));
    }
}
