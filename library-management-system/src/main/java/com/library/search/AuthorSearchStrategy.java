package com.library.search;

import com.library.domain.Book;

import java.util.Locale;

/** Case-insensitive partial match on the author. */
public class AuthorSearchStrategy implements BookSearchStrategy {
    @Override
    public boolean matches(Book book, String query) {
        return book.author().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT).trim());
    }
}
