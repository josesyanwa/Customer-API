package com.example.banking.controller;

import com.example.banking.dto.BalanceResponse;
import com.example.banking.service.BankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final BankingService bankingService;

    public AccountController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @GetMapping("/{accountNumber}/balance")
    public BalanceResponse getBalance(@PathVariable String accountNumber) {
        return bankingService.getBalance(accountNumber);
    }
}
