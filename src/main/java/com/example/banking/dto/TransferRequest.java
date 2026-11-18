package com.example.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank @Size(max = 100) String referenceId,
        @NotBlank @Size(max = 32) String sourceAccountNumber,
        @NotBlank @Size(max = 32) String destinationAccountNumber,
        @NotNull @Positive @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount) {
}
