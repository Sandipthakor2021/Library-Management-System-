package com.library.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract library member. Subclasses only differ in their lending policy (polymorphism); everything else,
 * including the borrowing history, is encapsulated here and exposed through read-only views.
 */
public abstract class Patron {
    private final String id;
    private String name;
    private String email;
    private String phone;
    private final Set<String> preferredGenres = new LinkedHashSet<>();
    private final List<LoanRecord> history = new ArrayList<>();

    protected Patron(String id, String name, String email, String phone) {
        this.id = id;
        updateContact(name, email, phone);
    }

    public abstract PatronType getType();

    /** Maximum number of books this patron may hold at once. */
    public abstract int getMaxActiveLoans();

    /** Loan period in days. */
    public abstract int getLoanPeriodDays();

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    public final void updateContact(String name, String email, String phone) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Patron name must not be blank");
        if (email == null || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
        this.name = name.trim();
        this.email = email.trim().toLowerCase();
        this.phone = phone == null ? "" : phone.trim();
    }

    public Set<String> getPreferredGenres() {
        return Collections.unmodifiableSet(preferredGenres);
    }

    public void setPreferredGenres(Set<String> genres) {
        preferredGenres.clear();
        genres.stream().filter(g -> g != null && !g.isBlank()).map(String::trim).forEach(preferredGenres::add);
    }

    public void addLoan(LoanRecord loan) {
        history.add(loan);
    }

    /** Full borrowing history, oldest first. */
    public List<LoanRecord> getBorrowingHistory() {
        return Collections.unmodifiableList(history);
    }

    public List<LoanRecord> getActiveLoans() {
        return history.stream().filter(LoanRecord::isActive).toList();
    }

    public boolean hasOverdueLoans(LocalDate today) {
        return history.stream().anyMatch(l -> l.isOverdue(today));
    }

    @Override
    public String toString() {
        return getType() + "Patron{" + id + ", " + name + ", " + email + "}";
    }
}
