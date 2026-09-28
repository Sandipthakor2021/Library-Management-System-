package com.library.repository;

import com.library.domain.BookCopy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Keeps a secondary index isbn -> copy ids so per-title lookups do not scan every copy. */
public class InMemoryCopyRepository extends InMemoryRepository<BookCopy, String> implements CopyRepository {
    private final Map<String, Set<String>> idsByIsbn = new HashMap<>();

    public InMemoryCopyRepository() {
        super(BookCopy::getId);
    }

    @Override
    public synchronized void save(BookCopy copy) {
        super.save(copy);
        idsByIsbn.computeIfAbsent(copy.getIsbn(), k -> new LinkedHashSet<>()).add(copy.getId());
    }

    @Override
    public synchronized boolean deleteById(String id) {
        BookCopy removed = store.remove(id);
        if (removed == null) return false;
        Set<String> ids = idsByIsbn.get(removed.getIsbn());
        if (ids != null) {
            ids.remove(id);
            if (ids.isEmpty()) idsByIsbn.remove(removed.getIsbn());
        }
        return true;
    }

    @Override
    public synchronized List<BookCopy> findByIsbn(String isbn) {
        List<BookCopy> result = new ArrayList<>();
        for (String id : idsByIsbn.getOrDefault(isbn, Set.of())) {
            result.add(store.get(id));
        }
        return result;
    }

    @Override
    public synchronized List<BookCopy> findByBranch(String branchId) {
        return store.values().stream().filter(c -> c.getBranchId().equals(branchId)).toList();
    }
}
