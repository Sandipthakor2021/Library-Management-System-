package com.library.event;

/** Observer contract. */
public interface LibraryEventListener {
    void onEvent(LibraryEvent event);
}
