package com.library.service;

import com.library.domain.LoanRecord;
import com.library.domain.Patron;
import com.library.domain.PatronFactory;
import com.library.domain.PatronType;
import com.library.exception.DuplicateEntityException;
import com.library.exception.EntityNotFoundException;
import com.library.repository.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class PatronService {
    private static final Logger LOG = Logger.getLogger(PatronService.class.getName());

    private final Repository<Patron, String> patrons;
    private final PatronFactory factory;

    public PatronService(Repository<Patron, String> patrons, PatronFactory factory) {
        this.patrons = patrons;
        this.factory = factory;
    }

    public Patron register(PatronType type, String name, String email, String phone) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (patrons.findAll().stream().anyMatch(p -> p.getEmail().equals(normalizedEmail))) {
            throw new DuplicateEntityException("Patron with email", normalizedEmail);
        }
        Patron patron = factory.create(type, name, email, phone);
        patrons.save(patron);
        LOG.info("Patron registered: " + patron.getId() + " (" + type + ") " + patron.getName());
        return patron;
    }

    public Patron updateContact(String patronId, String name, String email, String phone) {
        Patron patron = get(patronId);
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (patrons.findAll().stream()
                .anyMatch(p -> !p.getId().equals(patronId) && p.getEmail().equals(normalizedEmail))) {
            throw new DuplicateEntityException("Patron with email", normalizedEmail);
        }
        patron.updateContact(name, email, phone);
        LOG.info("Patron updated: " + patronId);
        return patron;
    }

    public Patron updatePreferences(String patronId, Set<String> preferredGenres) {
        Patron patron = get(patronId);
        patron.setPreferredGenres(preferredGenres);
        LOG.info("Patron preferences updated: " + patronId + " " + patron.getPreferredGenres());
        return patron;
    }

    public Patron get(String patronId) {
        return patrons.findById(patronId).orElseThrow(() -> new EntityNotFoundException("Patron", patronId));
    }

    public List<Patron> list() {
        return patrons.findAll().stream().sorted(Comparator.comparing(Patron::getId)).toList();
    }

    public List<LoanRecord> getBorrowingHistory(String patronId) {
        return get(patronId).getBorrowingHistory();
    }
}
