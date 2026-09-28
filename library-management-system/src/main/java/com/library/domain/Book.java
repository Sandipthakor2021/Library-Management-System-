package com.library.domain;

import java.time.Year;

/**
 * Catalogue entry describing a title. Immutable: an "update" produces a new value via {@link #toBuilder()}.
 * Physical items are modelled separately by {@link BookCopy}.
 */
public record Book(String isbn, String title, String author, int publicationYear, String genre) {

    public Book {
        isbn = normalizeIsbn(isbn);
        title = requireText(title, "title");
        author = requireText(author, "author");
        genre = (genre == null || genre.isBlank()) ? "General" : genre.trim();
        if (publicationYear <= 0 || publicationYear > Year.now().getValue() + 1) {
            throw new IllegalArgumentException("Invalid publication year: " + publicationYear);
        }
    }

    /** Removes hyphens/spaces so "978-0-13-468599-1" and "9780134685991" are the same key. */
    public static String normalizeIsbn(String raw) {
        String isbn = requireText(raw, "isbn").replaceAll("[\\s-]", "");
        if (!isbn.matches("\\d{9}[\\dXx]|\\d{13}")) {
            throw new IllegalArgumentException("ISBN must be 10 or 13 characters: " + raw);
        }
        return isbn.toUpperCase();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().isbn(isbn).title(title).author(author).publicationYear(publicationYear).genre(genre);
    }

    /** Builder pattern: readable construction and copy-with-changes for the immutable record. */
    public static final class Builder {
        private String isbn;
        private String title;
        private String author;
        private int publicationYear;
        private String genre;

        public Builder isbn(String v) { this.isbn = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder author(String v) { this.author = v; return this; }
        public Builder publicationYear(int v) { this.publicationYear = v; return this; }
        public Builder genre(String v) { this.genre = v; return this; }

        public Book build() {
            return new Book(isbn, title, author, publicationYear, genre);
        }
    }
}
