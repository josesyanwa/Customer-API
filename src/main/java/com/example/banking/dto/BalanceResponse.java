package com.example.banking.dto;

import com.example.banking.entity.Account;
import java.time.Instant;

public record BalanceResponse(
        String accountNumber,
        String accountHolderName,
        String balance,
        String currency,
        Instant asOf) {

    public static BalanceResponse from(Account account) {
        return new BalanceResponse(
                account.getAccountNumber(),
                account.getCustomerName(),
                account.getBalance().setScale(2).toPlainString(),
                account.getCurrency(),
                account.getUpdatedAt());
    }
}
