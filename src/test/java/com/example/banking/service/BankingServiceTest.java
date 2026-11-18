package com.example.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.banking.dto.CreateAccountRequest;
import com.example.banking.dto.RecordTransactionRequest;
import com.example.banking.dto.TransferRequest;
import com.example.banking.entity.Account;
import com.example.banking.entity.BankingTransaction;
import com.example.banking.entity.TransactionType;
import com.example.banking.exception.DuplicateAccountException;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.InvalidTransactionException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BankingServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private BankingService bankingService;

    @BeforeEach
    void setUp() {
        bankingService = new BankingService(accountRepository, transactionRepository);
    }

    @Test
    void createAccountSavesZeroBalanceAccount() {
        when(accountRepository.existsByAccountNumber("2000000001")).thenReturn(false);
        when(accountRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = bankingService.createAccount(
                new CreateAccountRequest("2000000001", "Casey Example", "USD"));

        assertEquals("2000000001", response.accountNumber());
        assertEquals("Casey Example", response.accountHolderName());
        assertEquals("0.00", response.balance());
        assertEquals("USD", response.currency());
        verify(accountRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(account ->
                account.getBalance().equals(new BigDecimal("0.00"))));
    }

    @Test
    void createAccountRejectsExistingAccountNumber() {
        when(accountRepository.existsByAccountNumber("2000000001")).thenReturn(true);

        assertThrows(DuplicateAccountException.class, () -> bankingService.createAccount(
                new CreateAccountRequest("2000000001", "Casey Example", "USD")));
    }

    @Test
    void depositCreditsAccountAndRecordsTransaction() {
        Account account = new Account("1000000001", "Alex Morgan", new BigDecimal("100.00"), "USD");
        when(transactionRepository.existsByReferenceId("deposit-1")).thenReturn(false);
        when(accountRepository.findByAccountNumberForUpdate("1000000001")).thenReturn(Optional.of(account));
        when(transactionRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(BankingTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        bankingService.recordTransaction(new RecordTransactionRequest(
                "deposit-1", "1000000001", new BigDecimal("25.50"), TransactionType.DEPOSIT, "Cash deposit"));

        assertEquals(new BigDecimal("125.50"), account.getBalance());
        verify(transactionRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(
                transaction -> transaction.getType() == TransactionType.DEPOSIT
                        && transaction.getDestinationAccountNumber().equals("1000000001")));
    }

    @Test
    void transferMovesFundsAndRecordsTransaction() {
        Account source = new Account("1000000001", "Alex Morgan", new BigDecimal("100.00"), "USD");
        Account destination = new Account("1000000002", "Jordan Lee", new BigDecimal("30.00"), "USD");
        when(transactionRepository.existsByReferenceId("transfer-1")).thenReturn(false);
        when(accountRepository.findByAccountNumberForUpdate("1000000001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumberForUpdate("1000000002")).thenReturn(Optional.of(destination));
        when(transactionRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(BankingTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        bankingService.transfer(new TransferRequest(
                "transfer-1", "1000000001", "1000000002", new BigDecimal("40.00")));

        assertEquals(new BigDecimal("60.00"), source.getBalance());
        assertEquals(new BigDecimal("70.00"), destination.getBalance());
        ArgumentCaptor<BankingTransaction> captor = ArgumentCaptor.forClass(BankingTransaction.class);
        verify(transactionRepository).saveAndFlush(captor.capture());
        assertEquals(TransactionType.TRANSFER, captor.getValue().getType());
        assertEquals("1000000001", captor.getValue().getSourceAccountNumber());
    }

    @Test
    void transferRejectsInsufficientFundsWithoutChangingBalances() {
        Account source = new Account("1000000001", "Alex Morgan", new BigDecimal("10.00"), "USD");
        Account destination = new Account("1000000002", "Jordan Lee", new BigDecimal("30.00"), "USD");
        when(transactionRepository.existsByReferenceId("transfer-2")).thenReturn(false);
        when(accountRepository.findByAccountNumberForUpdate("1000000001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumberForUpdate("1000000002")).thenReturn(Optional.of(destination));

        assertThrows(InsufficientFundsException.class, () -> bankingService.transfer(new TransferRequest(
                "transfer-2", "1000000001", "1000000002", new BigDecimal("40.00"))));

        assertEquals(new BigDecimal("10.00"), source.getBalance());
        assertEquals(new BigDecimal("30.00"), destination.getBalance());
    }

    @Test
    void transferRejectsSameAccount() {
        assertThrows(InvalidTransactionException.class, () -> bankingService.transfer(new TransferRequest(
                "transfer-3", "1000000001", "1000000001", new BigDecimal("1.00"))));
    }
}
