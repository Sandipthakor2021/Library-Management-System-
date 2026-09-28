package com.library.recommendation;

import com.library.domain.Book;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Keeps the k best-scoring books with a bounded min-heap: O(n log k) instead of sorting all n. */
final class TopK {
    private TopK() { }

    static List<Book> select(Map<Book, Double> scores, int k) {
        // Heap head = current worst entry; ties broken by title so results are deterministic.
        Comparator<Map.Entry<Book, Double>> worstFirst = Map.Entry.<Book, Double>comparingByValue()
                .thenComparing(e -> e.getKey().title(), Comparator.reverseOrder());
        PriorityQueue<Map.Entry<Book, Double>> heap = new PriorityQueue<>(worstFirst);
        for (Map.Entry<Book, Double> e : scores.entrySet()) {
            heap.offer(e);
            if (heap.size() > k) heap.poll();
        }
        List<Book> result = new ArrayList<>();
        while (!heap.isEmpty()) result.add(heap.poll().getKey());
        Collections.reverse(result);
        return result;
    }
}
