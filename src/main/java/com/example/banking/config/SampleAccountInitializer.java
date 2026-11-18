package com.example.banking.config;

import com.example.banking.entity.Account;
import com.example.banking.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean; 
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleAccountInitializer {

    @Bean
    CommandLineRunner initializeSampleAccounts(AccountRepository accountRepository) {
        return args -> {
            List<Account> samples = List.of(
                    new Account("1000000001", "Alex Morgan", new BigDecimal("1250.00"), "USD"),
                    new Account("1000000002", "Jordan Lee", new BigDecimal("800.00"), "USD"),
                    new Account("1000000003", "Taylor Kim", new BigDecimal("2500.00"), "USD"));
            samples.stream()
                    .filter(account -> !accountRepository.existsByAccountNumber(account.getAccountNumber()))
                    .forEach(accountRepository::save);
        };
    }
}
