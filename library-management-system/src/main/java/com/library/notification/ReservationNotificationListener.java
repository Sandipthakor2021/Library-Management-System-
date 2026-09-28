package com.library.notification;

import com.library.domain.Book;
import com.library.domain.Patron;
import com.library.event.LibraryEvent;
import com.library.event.LibraryEventListener;
import com.library.repository.Repository;

/** Observer: tells a patron when the book they reserved is waiting for them. */
public class ReservationNotificationListener implements LibraryEventListener {
    private final Repository<Patron, String> patrons;
    private final Repository<Book, String> books;
    private final NotificationService notifications;

    public ReservationNotificationListener(Repository<Patron, String> patrons, Repository<Book, String> books,
                                           NotificationService notifications) {
        this.patrons = patrons;
        this.books = books;
        this.notifications = notifications;
    }

    @Override
    public void onEvent(LibraryEvent event) {
        if (event instanceof LibraryEvent.ReservationReady ready) {
            Patron patron = patrons.findById(ready.reservation().getPatronId()).orElse(null);
            Book book = books.findById(ready.reservation().getIsbn()).orElse(null);
            if (patron != null && book != null) {
                notifications.notify(patron, "\"" + book.title() + "\" is now available for you at branch "
                        + ready.copy().getBranchId() + ".");
            }
        }
    }
}
