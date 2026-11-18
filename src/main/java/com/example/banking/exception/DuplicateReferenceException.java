package com.example.banking.exception;

public class DuplicateReferenceException extends RuntimeException {

    public DuplicateReferenceException(String referenceId) {
        super("A transaction with referenceId '" + referenceId + "' already exists");
    }
}
