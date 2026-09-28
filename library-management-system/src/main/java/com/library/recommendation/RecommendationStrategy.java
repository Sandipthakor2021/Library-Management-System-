package com.library.recommendation;

import com.library.domain.Book;
import com.library.domain.Patron;

import java.util.List;

/** Strategy pattern: interchangeable ways of ranking books for a patron. */
public interface RecommendationStrategy {
    /** @return at most {@code limit} books, best first, never including books the patron already borrowed. */
    List<Book> recommend(Patron patron, int limit);
}
