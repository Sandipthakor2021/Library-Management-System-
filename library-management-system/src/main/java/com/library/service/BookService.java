package com.library.service;

import com.library.domain.Book;
import com.library.domain.BookCopy;
import com.library.event.EventPublisher;
import com.library.event.LibraryEvent;
import com.library.exception.BusinessRuleException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.EntityNotFoundException;
import com.library.repository.CopyRepository;
import com.library.repository.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/** Catalogue management: add, update and remove titles. Search lives in {@code BookSearchService}. */
public class BookService {
    private static final Logger LOG = Logger.getLogger(BookService.class.getName());

    private final Repository<Book, String> books;
    private final CopyRepository copies;
    private final EventPublisher events;

    public BookService(Repository<Book, String> books, CopyRepository copies, EventPublisher events) {
        this.books = books;
        this.copies = copies;
        this.events = events;
    }

    public Book addBook(Book book) {
        if (books.existsById(book.isbn())) {
            throw new DuplicateEntityException("Book", book.isbn());
        }
        books.save(book);
        LOG.info("Book added: " + book.isbn() + " \"" + book.title() + "\"");
        return book;
    }

    /** Replaces the catalogue data for an existing ISBN. */
    public Book updateBook(Book updated) {
        if (!books.existsById(updated.isbn())) {
            throw new EntityNotFoundException("Book", updated.isbn());
        }
        books.save(updated);
        LOG.info("Book updated: " + updated.isbn());
        return updated;
    }

    /** Removes the title and all its copies, unless any copy is currently out, on hold or in transit. */
    public void removeBook(String isbn) {
        Book book = getBook(isbn);
        List<BookCopy> titleCopies = copies.findByIsbn(book.isbn());
        if (titleCopies.stream().anyMatch(c -> !c.isAvailable())) {
            throw new BusinessRuleException("Cannot remove " + isbn + ": some copies are borrowed, on hold or in transit");
        }
        titleCopies.forEach(c -> copies.deleteById(c.getId()));
        books.deleteById(book.isbn());
        LOG.info("Book removed: " + isbn + " (" + titleCopies.size() + " copies deleted)");
        events.publish(new LibraryEvent.BookRemoved(book));
    }

    public Book getBook(String isbn) {
        String key = Book.normalizeIsbn(isbn);
        return books.findById(key).orElseThrow(() -> new EntityNotFoundException("Book", key));
    }

    public List<Book> listBooks() {
        return books.findAll().stream().sorted(Comparator.comparing(Book::title)).toList();
    }
}
