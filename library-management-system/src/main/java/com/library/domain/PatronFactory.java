package com.library.domain;

import java.util.concurrent.atomic.AtomicInteger;

/** Factory pattern: callers pick a {@link PatronType}; construction details and id generation live here. */
public class PatronFactory {
    private final AtomicInteger sequence = new AtomicInteger();

    public Patron create(PatronType type, String name, String email, String phone) {
        String id = String.format("P-%04d", sequence.incrementAndGet());
        return switch (type) {
            case STUDENT -> new StudentPatron(id, name, email, phone);
            case STANDARD -> new StandardPatron(id, name, email, phone);
            case PREMIUM -> new PremiumPatron(id, name, email, phone);
        };
    }
}
