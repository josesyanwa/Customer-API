package com.example.banking.exception;

public class DuplicateAccountException extends RuntimeException {

    public DuplicateAccountException(String accountNumber) {
        super("Account '" + accountNumber + "' already exists");
    }
}
