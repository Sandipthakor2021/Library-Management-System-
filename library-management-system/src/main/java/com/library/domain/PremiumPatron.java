package com.library.domain;

/** Premium membership: up to 10 books for 30 days. */
public class PremiumPatron extends Patron {

    public PremiumPatron(String id, String name, String email, String phone) {
        super(id, name, email, phone);
    }

    @Override
    public PatronType getType() { return PatronType.PREMIUM; }

    @Override
    public int getMaxActiveLoans() { return 10; }

    @Override
    public int getLoanPeriodDays() { return 30; }
}
