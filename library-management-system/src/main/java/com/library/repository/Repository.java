package com.library.repository;

import java.util.List;
import java.util.Optional;

/** Persistence abstraction (Dependency Inversion): services depend on this, not on a concrete store. */
public interface Repository<T, ID> {
    void save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    boolean existsById(ID id);

    boolean deleteById(ID id);
}
