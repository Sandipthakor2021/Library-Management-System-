package com.library.search;

import com.library.domain.Book;

import java.util.Locale;

/** Case-insensitive partial match on the title. */
public class TitleSearchStrategy implements BookSearchStrategy {
    @Override
    public boolean matches(Book book, String query) {
        return book.title().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT).trim());
    }
}
