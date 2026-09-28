package com.library.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Thread-safe, insertion-ordered map-backed repository. */
public class InMemoryRepository<T, ID> implements Repository<T, ID> {
    protected final Map<ID, T> store = new LinkedHashMap<>();
    private final Function<T, ID> idExtractor;

    public InMemoryRepository(Function<T, ID> idExtractor) {
        this.idExtractor = idExtractor;
    }

    @Override
    public synchronized void save(T entity) {
        store.put(idExtractor.apply(entity), entity);
    }

    @Override
    public synchronized Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public synchronized List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public synchronized boolean existsById(ID id) {
        return store.containsKey(id);
    }

    @Override
    public synchronized boolean deleteById(ID id) {
        return store.remove(id) != null;
    }
}
