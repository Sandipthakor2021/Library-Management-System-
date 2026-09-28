package com.library.service;

import com.library.domain.Branch;
import com.library.exception.DuplicateEntityException;
import com.library.exception.EntityNotFoundException;
import com.library.repository.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public class BranchService {
    private static final Logger LOG = Logger.getLogger(BranchService.class.getName());

    private final Repository<Branch, String> branches;

    public BranchService(Repository<Branch, String> branches) {
        this.branches = branches;
    }

    public Branch addBranch(String id, String name, String address) {
        Branch branch = new Branch(id, name, address);
        if (branches.existsById(id)) {
            throw new DuplicateEntityException("Branch", id);
        }
        branches.save(branch);
        LOG.info("Branch added: " + id + " (" + name + ")");
        return branch;
    }

    public Branch get(String id) {
        return branches.findById(id).orElseThrow(() -> new EntityNotFoundException("Branch", id));
    }

    public List<Branch> list() {
        return branches.findAll().stream().sorted(Comparator.comparing(Branch::id)).toList();
    }
}
