package com.library.repository;

import com.library.domain.BookCopy;

import java.util.List;

public interface CopyRepository extends Repository<BookCopy, String> {
    List<BookCopy> findByIsbn(String isbn);

    List<BookCopy> findByBranch(String branchId);
}
