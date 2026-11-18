package com.example.banking.dto;

import com.example.banking.entity.BankingTransaction;
import com.example.banking.entity.TransactionStatus;
import com.example.banking.entity.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String referenceId,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        Instant createdAt) {

    public static TransactionResponse from(BankingTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getReferenceId(),
                transaction.getSourceAccountNumber(),
                transaction.getDestinationAccountNumber(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCreatedAt());
    }
}
