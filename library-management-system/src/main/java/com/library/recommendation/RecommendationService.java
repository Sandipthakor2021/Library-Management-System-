package com.library.recommendation;

import com.library.domain.Book;
import com.library.domain.Patron;
import com.library.exception.EntityNotFoundException;
import com.library.repository.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Consults strategies in priority order and tops up the list from the next one until {@code limit} is reached.
 * Strategies are injected, so new algorithms can be added without modifying this class.
 */
public class RecommendationService {
    private static final Logger LOG = Logger.getLogger(RecommendationService.class.getName());

    private final Repository<Patron, String> patrons;
    private final List<RecommendationStrategy> strategies;

    public RecommendationService(Repository<Patron, String> patrons, List<RecommendationStrategy> strategies) {
        this.patrons = patrons;
        this.strategies = List.copyOf(strategies);
    }

    public List<Book> recommend(String patronId, int limit) {
        Patron patron = patrons.findById(patronId)
                .orElseThrow(() -> new EntityNotFoundException("Patron", patronId));
        Set<Book> result = new LinkedHashSet<>();
        for (RecommendationStrategy strategy : strategies) {
            if (result.size() >= limit) break;
            result.addAll(strategy.recommend(patron, limit));
        }
        List<Book> top = result.stream().limit(limit).toList();
        LOG.info("Generated " + top.size() + " recommendation(s) for " + patronId);
        return top;
    }
}
