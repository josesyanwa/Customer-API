package com.example.banking.dto;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        int status,
        Instant timestamp,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}
