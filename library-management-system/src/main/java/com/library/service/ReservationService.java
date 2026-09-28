package com.library.service;

import com.library.domain.BookCopy;
import com.library.domain.Patron;
import com.library.domain.Reservation;
import com.library.domain.ReservationStatus;
import com.library.event.EventPublisher;
import com.library.event.LibraryEvent;
import com.library.event.LibraryEventListener;
import com.library.exception.BusinessRuleException;
import com.library.exception.EntityNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * FIFO reservation queue per ISBN. It is also an Observer: when a copy becomes available it moves it to the
 * hold shelf for the first waiting patron and publishes {@code ReservationReady}, which the notification
 * listener turns into a message. Lending and transfers never call this class directly.
 */
public class ReservationService implements LibraryEventListener {
    private static final Logger LOG = Logger.getLogger(ReservationService.class.getName());

    private final PatronService patrons;
    private final BookService books;
    private final InventoryService inventory;
    private final EventPublisher events;
    private final Clock clock;
    private final Map<String, Reservation> byId = new HashMap<>();
    private final Map<String, Deque<Reservation>> queueByIsbn = new HashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    public ReservationService(PatronService patrons, BookService books, InventoryService inventory,
                              EventPublisher events, Clock clock) {
        this.patrons = patrons;
        this.books = books;
        this.inventory = inventory;
        this.events = events;
        this.clock = clock;
    }

    public synchronized Reservation reserve(String patronId, String isbn) {
        Patron patron = patrons.get(patronId);
        String key = books.getBook(isbn).isbn();
        List<BookCopy> copies = inventory.copiesOf(key);

        if (copies.isEmpty()) {
            throw new BusinessRuleException("No copies of " + key + " exist to reserve");
        }
        if (copies.stream().anyMatch(BookCopy::isAvailable)) {
            throw new BusinessRuleException("A copy of " + key + " is available now; check it out instead");
        }
        if (patron.getActiveLoans().stream().anyMatch(l -> l.getIsbn().equals(key))) {
            throw new BusinessRuleException(patron.getName() + " already has " + key + " on loan");
        }
        Deque<Reservation> queue = queueByIsbn.computeIfAbsent(key, k -> new ArrayDeque<>());
        if (queue.stream().anyMatch(r -> r.getPatronId().equals(patronId))) {
            throw new BusinessRuleException(patron.getName() + " already has a reservation for " + key);
        }

        Reservation reservation = new Reservation(String.format("R-%04d", sequence.incrementAndGet()), key, patronId,
                Instant.now(clock));
        queue.addLast(reservation);
        byId.put(reservation.getId(), reservation);
        LOG.info("RESERVE " + reservation.getId() + ": " + patron.getName() + " queued for " + key
                + " (position " + queue.size() + ")");
        return reservation;
    }

    public synchronized void cancel(String reservationId) {
        Reservation reservation = byId.get(reservationId);
        if (reservation == null) throw new EntityNotFoundException("Reservation", reservationId);
        if (!reservation.isOpen()) {
            throw new BusinessRuleException("Reservation " + reservationId + " is already " + reservation.getStatus());
        }
        BookCopy heldCopy = reservation.getStatus() == ReservationStatus.READY
                ? inventory.getCopy(reservation.getHeldCopyId()) : null;

        reservation.cancel();
        queueByIsbn.getOrDefault(reservation.getIsbn(), new ArrayDeque<>()).remove(reservation);
        LOG.info("Reservation cancelled: " + reservationId);

        if (heldCopy != null) {
            heldCopy.releaseHold();
            events.publish(new LibraryEvent.CopyAvailable(heldCopy)); // offer it to the next patron in line
        }
    }

    /** Open reservations for a title in queue order. */
    public synchronized List<Reservation> getQueue(String isbn) {
        return new ArrayList<>(queueByIsbn.getOrDefault(books.getBook(isbn).isbn(), new ArrayDeque<>()));
    }

    public synchronized List<Reservation> getReservationsForPatron(String patronId) {
        return byId.values().stream().filter(r -> r.getPatronId().equals(patronId))
                .sorted(Comparator.comparing(Reservation::getId)).toList();
    }

    @Override
    public synchronized void onEvent(LibraryEvent event) {
        if (event instanceof LibraryEvent.CopyAvailable e) {
            holdForNextInQueue(e.copy());
        } else if (event instanceof LibraryEvent.BookCheckedOut e) {
            completeReservationIfAny(e);
        } else if (event instanceof LibraryEvent.BookRemoved e) {
            Deque<Reservation> queue = queueByIsbn.remove(e.book().isbn());
            if (queue != null) queue.forEach(Reservation::cancel);
        }
    }

    private void holdForNextInQueue(BookCopy copy) {
        if (!copy.isAvailable()) return;
        Deque<Reservation> queue = queueByIsbn.get(copy.getIsbn());
        if (queue == null) return;
        queue.stream().filter(r -> r.getStatus() == ReservationStatus.PENDING).findFirst().ifPresent(next -> {
            copy.placeHold(next.getPatronId());
            next.markReady(copy.getId());
            LOG.info("HOLD copy " + copy.getId() + " held for " + next.getPatronId() + " (" + next.getId() + ")");
            events.publish(new LibraryEvent.ReservationReady(next, copy));
        });
    }

    private void completeReservationIfAny(LibraryEvent.BookCheckedOut e) {
        Deque<Reservation> queue = queueByIsbn.get(e.copy().getIsbn());
        if (queue == null) return;
        queue.stream()
                .filter(r -> r.getStatus() == ReservationStatus.READY
                        && r.getPatronId().equals(e.loan().getPatronId())
                        && e.copy().getId().equals(r.getHeldCopyId()))
                .findFirst().ifPresent(r -> {
                    r.markFulfilled();
                    queue.remove(r);
                    LOG.info("Reservation fulfilled: " + r.getId());
                });
    }
}
