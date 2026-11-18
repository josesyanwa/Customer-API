package com.example.banking.controller;

import com.example.banking.dto.BalanceResponse;
import com.example.banking.dto.CreateAccountRequest;
import com.example.banking.service.BankingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final BankingService bankingService;

    public AccountController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BalanceResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        logger.info("Received account creation request: accountNumber={}", request.accountNumber());
        return bankingService.createAccount(request);
    }

    @GetMapping("/{accountNumber}/balance")
    public BalanceResponse getBalance(@PathVariable String accountNumber) {
        logger.info("Received balance request: accountNumber={}", accountNumber);
        return bankingService.getBalance(accountNumber);
    }
}
