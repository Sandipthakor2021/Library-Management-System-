package com.library.service;

import com.library.domain.BookCopy;
import com.library.event.EventPublisher;
import com.library.event.LibraryEvent;
import com.library.exception.BusinessRuleException;

import java.util.logging.Logger;

/** Moves copies between branches in two steps (dispatch, then receive) so in-transit copies are tracked. */
public class TransferService {
    private static final Logger LOG = Logger.getLogger(TransferService.class.getName());

    private final InventoryService inventory;
    private final BranchService branches;
    private final EventPublisher events;

    public TransferService(InventoryService inventory, BranchService branches, EventPublisher events) {
        this.inventory = inventory;
        this.branches = branches;
        this.events = events;
    }

    public synchronized BookCopy dispatch(String copyId, String toBranchId) {
        BookCopy copy = inventory.getCopy(copyId);
        branches.get(toBranchId);
        if (copy.getBranchId().equals(toBranchId)) {
            throw new BusinessRuleException("Copy " + copyId + " is already at branch " + toBranchId);
        }
        copy.startTransit(toBranchId);
        LOG.info("TRANSFER dispatched: " + copyId + " " + copy.getBranchId() + " -> " + toBranchId);
        return copy;
    }

    public synchronized BookCopy receive(String copyId) {
        BookCopy copy = inventory.getCopy(copyId);
        String from = copy.getBranchId();
        String to = copy.getTransitDestination();
        copy.completeTransit();
        LOG.info("TRANSFER received: " + copyId + " arrived at " + copy.getBranchId());
        events.publish(new LibraryEvent.CopyTransferred(copy, from, to));
        events.publish(new LibraryEvent.CopyAvailable(copy));
        return copy;
    }

    /** Convenience: dispatch and receive in one call (e.g. a courier hand-over that is already confirmed). */
    public BookCopy transfer(String copyId, String toBranchId) {
        dispatch(copyId, toBranchId);
        return receive(copyId);
    }
}
