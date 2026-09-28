package com.library.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Observer pattern subject. A failing listener is logged and never breaks the publisher or other listeners. */
public class EventPublisher {
    private static final Logger LOG = Logger.getLogger(EventPublisher.class.getName());

    private final List<LibraryEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(LibraryEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(LibraryEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(LibraryEvent event) {
        LOG.fine(() -> "Publishing " + event.getClass().getSimpleName());
        for (LibraryEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Listener " + listener.getClass().getSimpleName()
                        + " failed on " + event.getClass().getSimpleName(), e);
            }
        }
    }
}
