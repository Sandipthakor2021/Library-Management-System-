package com.library.search;

import com.library.domain.Book;
import com.library.repository.Repository;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Open/Closed: a new search field is a new {@link BookSearchStrategy} registered in the map; this class
 * does not change.
 */
public class BookSearchService {
    private static final Logger LOG = Logger.getLogger(BookSearchService.class.getName());

    private final Repository<Book, String> books;
    private final Map<SearchType, BookSearchStrategy> strategies = new EnumMap<>(SearchType.class);

    public BookSearchService(Repository<Book, String> books) {
        this.books = books;
        strategies.put(SearchType.TITLE, new TitleSearchStrategy());
        strategies.put(SearchType.AUTHOR, new AuthorSearchStrategy());
        strategies.put(SearchType.ISBN, new IsbnSearchStrategy());
    }

    public List<Book> search(SearchType type, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        BookSearchStrategy strategy = strategies.get(type);
        List<Book> result = books.findAll().stream()
                .filter(b -> strategy.matches(b, query))
                .sorted(Comparator.comparing(Book::title))
                .toList();
        LOG.fine(() -> "Search " + type + " '" + query + "' -> " + result.size() + " result(s)");
        return result;
    }

    public List<Book> byTitle(String q) { return search(SearchType.TITLE, q); }

    public List<Book> byAuthor(String q) { return search(SearchType.AUTHOR, q); }

    public List<Book> byIsbn(String q) { return search(SearchType.ISBN, q); }
}
