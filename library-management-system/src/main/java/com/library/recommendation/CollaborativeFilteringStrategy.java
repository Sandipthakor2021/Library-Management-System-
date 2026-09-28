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
 * "Patrons like you also borrowed": user-based collaborative filtering. Similarity between two patrons is the
 * Jaccard index of their borrowed-ISBN sets; a candidate's score is the sum of similarities of the patrons who borrowed it.
 */
public class CollaborativeFilteringStrategy implements RecommendationStrategy {
    private final Repository<Book, String> books;
    private final Repository<Patron, String> patrons;

    public CollaborativeFilteringStrategy(Repository<Book, String> books, Repository<Patron, String> patrons) {
        this.books = books;
        this.patrons = patrons;
    }

    @Override
    public List<Book> recommend(Patron patron, int limit) {
        Set<String> mine = borrowedIsbns(patron);
        if (mine.isEmpty()) return List.of();

        Map<String, Double> isbnScores = new HashMap<>();
        for (Patron other : patrons.findAll()) {
            if (other.getId().equals(patron.getId())) continue;
            Set<String> theirs = borrowedIsbns(other);
            double similarity = jaccard(mine, theirs);
            if (similarity == 0) continue;
            for (String isbn : theirs) {
                if (!mine.contains(isbn)) isbnScores.merge(isbn, similarity, Double::sum);
            }
        }

        Map<Book, Double> scores = new HashMap<>();
        isbnScores.forEach((isbn, score) -> books.findById(isbn).ifPresent(b -> scores.put(b, score)));
        return TopK.select(scores, limit);
    }

    private static Set<String> borrowedIsbns(Patron p) {
        Set<String> isbns = new HashSet<>();
        for (LoanRecord l : p.getBorrowingHistory()) isbns.add(l.getIsbn());
        return isbns;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        int intersection = 0;
        for (String x : a) if (b.contains(x)) intersection++;
        int union = a.size() + b.size() - intersection;
        return union == 0 ? 0 : (double) intersection / union;
    }
}
