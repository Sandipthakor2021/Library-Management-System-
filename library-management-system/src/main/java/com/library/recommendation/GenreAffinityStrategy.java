package com.library.recommendation;

import com.library.domain.Book;
import com.library.domain.LoanRecord;
import com.library.domain.Patron;
import com.library.repository.Repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Content-based: builds a genre/author weight profile from the patron's history plus their stated
 * genre preferences, then scores every unread book against it. Works for new patrons who only have preferences.
 */
public class GenreAffinityStrategy implements RecommendationStrategy {
    static final double PREFERENCE_WEIGHT = 2.0;
    static final double GENRE_WEIGHT = 1.0;
    static final double AUTHOR_WEIGHT = 0.5;

    private final Repository<Book, String> books;

    public GenreAffinityStrategy(Repository<Book, String> books) {
        this.books = books;
    }

    @Override
    public List<Book> recommend(Patron patron, int limit) {
        Set<String> read = new HashSet<>();
        Map<String, Double> genreProfile = new HashMap<>();
        Map<String, Double> authorProfile = new HashMap<>();

        for (String genre : patron.getPreferredGenres()) {
            genreProfile.merge(genre.toLowerCase(), PREFERENCE_WEIGHT, Double::sum);
        }
        for (LoanRecord loan : patron.getBorrowingHistory()) {
            read.add(loan.getIsbn());
            books.findById(loan.getIsbn()).ifPresent(b -> {
                genreProfile.merge(b.genre().toLowerCase(), GENRE_WEIGHT, Double::sum);
                authorProfile.merge(b.author().toLowerCase(), AUTHOR_WEIGHT, Double::sum);
            });
        }

        Map<Book, Double> scores = new HashMap<>();
        for (Book candidate : books.findAll()) {
            if (read.contains(candidate.isbn())) continue;
            double score = genreProfile.getOrDefault(candidate.genre().toLowerCase(), 0.0)
                    + authorProfile.getOrDefault(candidate.author().toLowerCase(), 0.0);
            if (score > 0) scores.put(candidate, score);
        }
        return TopK.select(scores, limit);
    }
}
