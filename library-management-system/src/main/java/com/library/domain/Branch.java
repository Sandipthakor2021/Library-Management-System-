package com.library.domain;

/** A physical library branch. */
public record Branch(String id, String name, String address) {
    public Branch {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Branch id and name are required");
        }
    }
}
