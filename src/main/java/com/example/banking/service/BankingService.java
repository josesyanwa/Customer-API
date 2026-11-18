package com.example.banking.service;

import com.example.banking.dto.BalanceResponse;
import com.example.banking.dto.CreateAccountRequest;
import com.example.banking.dto.RecordTransactionRequest;
import com.example.banking.dto.TransactionResponse;
import com.example.banking.dto.TransferRequest;
import com.example.banking.entity.Account;
import com.example.banking.entity.BankingTransaction;
import com.example.banking.entity.TransactionStatus;
import com.example.banking.entity.TransactionType;
import com.example.banking.exception.DuplicateAccountException;
import com.example.banking.exception.DuplicateReferenceException;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.InvalidTransactionException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankingService {

    private static final Logger logger = LoggerFactory.getLogger(BankingService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public BankingService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public BalanceResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByAccountNumber(request.accountNumber())) {
            throw new DuplicateAccountException(request.accountNumber());
        }
        Account account = new Account(
                request.accountNumber(), request.customerName(), BigDecimal.ZERO.setScale(2), request.currency());
        try {
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateAccountException(request.accountNumber());
        }
        logger.info("Account created: accountNumber={}, currency={}",
                account.getAccountNumber(), account.getCurrency());
        return BalanceResponse.from(account);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse recordTransaction(RecordTransactionRequest request) {
        if (request.type() != TransactionType.DEPOSIT) {
            throw new InvalidTransactionException("Only DEPOSIT transactions can be recorded at this endpoint");
        }
        ensureUniqueReference(request.referenceId());
        Account account = lockAccount(request.accountNumber());
        account.deposit(request.amount());
        BankingTransaction transaction = new BankingTransaction(
                request.referenceId(),
                null,
                account.getAccountNumber(),
                request.amount(),
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                request.description());
        try {
            transactionRepository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateReferenceException(request.referenceId());
        }
        logger.info("Deposit completed: referenceId={}, accountNumber={}, amount={}",
                request.referenceId(), account.getAccountNumber(), request.amount());
        return TransactionResponse.from(transaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse transfer(TransferRequest request) {
        if (request.sourceAccountNumber().equals(request.destinationAccountNumber())) {
            throw new InvalidTransactionException("Source and destination accounts must be different");
        }
        ensureUniqueReference(request.referenceId());

        List<String> accountNumbers = List.of(request.sourceAccountNumber(), request.destinationAccountNumber())
                .stream()
                .sorted()
                .toList();
        Account first = lockAccount(accountNumbers.get(0));
        Account second = lockAccount(accountNumbers.get(1));
        Account source = first.getAccountNumber().equals(request.sourceAccountNumber()) ? first : second;
        Account destination = first.getAccountNumber().equals(request.destinationAccountNumber()) ? first : second;

        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new InvalidTransactionException("Transfers between different currencies are not supported");
        }
        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account '" + source.getAccountNumber() + "'");
        }

        source.debit(request.amount());
        destination.deposit(request.amount());
        BankingTransaction transaction = new BankingTransaction(
                request.referenceId(),
                source.getAccountNumber(),
                destination.getAccountNumber(),
                request.amount(),
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                null);
        try {
            transactionRepository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateReferenceException(request.referenceId());
        }
        logger.info("Transfer completed: referenceId={}, source={}, destination={}, amount={}",
                request.referenceId(), source.getAccountNumber(), destination.getAccountNumber(), request.amount());
        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account '" + accountNumber + "' was not found"));
        return BalanceResponse.from(account);
    }

    private void ensureUniqueReference(String referenceId) {
        if (transactionRepository.existsByReferenceId(referenceId)) {
            throw new DuplicateReferenceException(referenceId);
        }
    }

    private Account lockAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account '" + accountNumber + "' was not found"));
    }
}
