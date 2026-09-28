package com.library.domain;

/** Student membership: up to 3 books for 14 days. */
public class StudentPatron extends Patron {

    public StudentPatron(String id, String name, String email, String phone) {
        super(id, name, email, phone);
    }

    @Override
    public PatronType getType() { return PatronType.STUDENT; }

    @Override
    public int getMaxActiveLoans() { return 3; }

    @Override
    public int getLoanPeriodDays() { return 14; }
}
