package com.example.banking.repository;

import com.example.banking.entity.BankingTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<BankingTransaction, UUID> {

    boolean existsByReferenceId(String referenceId);
}
