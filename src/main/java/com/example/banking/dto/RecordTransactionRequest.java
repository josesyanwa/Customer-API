package com.example.banking.dto;

import com.example.banking.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RecordTransactionRequest(
        @NotBlank @Size(max = 100) String referenceId,
        @NotBlank @Size(max = 32) String accountNumber,
        @NotNull @Positive @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotNull TransactionType type,
        @Size(max = 250) String description) {
}
