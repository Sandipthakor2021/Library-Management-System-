package com.library.search;

import com.library.domain.Book;

/** Strategy pattern: one implementation per searchable field. */
public interface BookSearchStrategy {
    boolean matches(Book book, String query);
}
