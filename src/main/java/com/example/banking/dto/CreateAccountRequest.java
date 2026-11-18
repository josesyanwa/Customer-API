package com.example.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 32) String accountNumber,
        @NotBlank @Size(max = 120) String customerName,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter uppercase currency code")
                String currency) {
}
