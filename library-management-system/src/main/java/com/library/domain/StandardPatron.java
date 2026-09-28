package com.library.domain;

/** Standard membership: up to 5 books for 21 days. */
public class StandardPatron extends Patron {

    public StandardPatron(String id, String name, String email, String phone) {
        super(id, name, email, phone);
    }

    @Override
    public PatronType getType() { return PatronType.STANDARD; }

    @Override
    public int getMaxActiveLoans() { return 5; }

    @Override
    public int getLoanPeriodDays() { return 21; }
}
