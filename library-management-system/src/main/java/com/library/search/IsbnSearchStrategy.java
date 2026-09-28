package com.library.search;

import com.library.domain.Book;

/** Exact match on the ISBN (hyphens and spaces in the query are ignored). */
public class IsbnSearchStrategy implements BookSearchStrategy {
    @Override
    public boolean matches(Book book, String query) {
        return book.isbn().equalsIgnoreCase(query.replaceAll("[\\s-]", ""));
    }
}
