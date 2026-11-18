
package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.application.command.TransferCommand;
import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.domain.entity.AccountStatus;
import br.com.tr.personal_finance_api.domain.entity.Transaction;
import br.com.tr.personal_finance_api.domain.entity.TransactionType;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.AccountRepository;
import br.com.tr.personal_finance_api.domain.repository.TransactionRepository;
import br.com.tr.personal_finance_api.domain.repository.TransferIdempotencyRepository;
import br.com.tr.personal_finance_api.domain.repository.TransferIdempotencyRepository.ReservationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferIdempotencyRepository transferIdempotencyRepository;

    @InjectMocks
    private TransferService transferService;

    private UUID userId;
    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        User owner = User.builder()
                .id(userId)
                .build();

        sourceAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC-100")
                .balance(new BigDecimal("1000.00"))
                .currency("BRL")
                .status(AccountStatus.ACTIVE)
                .user(owner)
                .build();

        destinationAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC-200")
                .balance(new BigDecimal("500.00"))
                .currency("BRL")
                .status(AccountStatus.ACTIVE)
                .user(owner)
                .build();
    }

    private void stubLockedAccounts() {
        when(accountRepository.findAllByAccountNumbersForUpdate(
                List.of("ACC-100", "ACC-200")
        )).thenReturn(List.of(sourceAccount, destinationAccount));
    }

    @Test
    void shouldTransferAmountAndSaveDebitAndCreditTransactions() {
        stubLockedAccounts();

        TransferCommand command = command(
                "ACC-100", "ACC-200",
                new BigDecimal("250.00"),
                "transfer-123", userId
        );

        when(transferIdempotencyRepository.reserveOrValidate(
                userId,
                "transfer-123",
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("250.00"),
                "BRL"
        )).thenReturn(ReservationResult.NEW_REQUEST);

        transferService.transfer(command);

        assertEquals(new BigDecimal("750.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("750.00"), destinationAccount.getBalance());

        verify(transactionRepository, times(2)).save(any(Transaction.class));

        verify(transactionRepository).save(argThat(transaction ->
                transaction.getTransactionType() == TransactionType.DEBIT
                        && transaction.getAccount() == sourceAccount
                        && transaction.getAmount().equals(new BigDecimal("250.00"))
        ));

        verify(transactionRepository).save(argThat(transaction ->
                transaction.getTransactionType() == TransactionType.CREDIT
                        && transaction.getAccount() == destinationAccount
                        && transaction.getAmount().equals(new BigDecimal("250.00"))
        ));
    }

    @Test
    void shouldNotExecuteTransferWhenRequestIsAnIdempotentReplay() {
        stubLockedAccounts();

        TransferCommand command = command(
                "ACC-100", "ACC-200",
                new BigDecimal("250.00"),
                "transfer-123", userId
        );

        when(transferIdempotencyRepository.reserveOrValidate(
                userId,
                "transfer-123",
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("250.00"),
                "BRL"
        )).thenReturn(ReservationResult.IDEMPOTENT_REPLAY);

        transferService.transfer(command);

        assertEquals(new BigDecimal("1000.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("500.00"), destinationAccount.getBalance());

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferWhenSourceAccountBelongsToAnotherUser() {
        stubLockedAccounts();

        TransferCommand command = command(
                "ACC-100", "ACC-200",
                new BigDecimal("100.00"),
                "transfer-unauthorized",
                UUID.randomUUID()
        );

        assertThrows(
                RuntimeException.class,
                () -> transferService.transfer(command)
        );

        assertEquals(new BigDecimal("1000.00"), sourceAccount.getBalance());

        verify(transferIdempotencyRepository, never())
                .reserveOrValidate(any(), any(), any(), any(), any(), any());

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferWhenSourceAccountHasInsufficientBalance() {
        stubLockedAccounts();

        TransferCommand command = command(
                "ACC-100", "ACC-200",
                new BigDecimal("1500.00"),
                null, userId
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(command)
        );

        assertEquals(new BigDecimal("1000.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("500.00"), destinationAccount.getBalance());

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferBetweenAccountsWithDifferentCurrencies() {
        stubLockedAccounts();
        destinationAccount.setCurrency("USD");

        TransferCommand command = command(
                "ACC-100", "ACC-200",
                new BigDecimal("100.00"),
                null, userId
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(command)
        );

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferWhenAnAccountIsNotActive() {
        stubLockedAccounts();
        destinationAccount.setStatus(AccountStatus.BLOCKED);

        TransferCommand command = command(
                "ACC-100", "ACC-200",
                new BigDecimal("100.00"),
                null, userId
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(command)
        );

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferBetweenTheSameAccount() {
        TransferCommand command = command(
                "ACC-100", "ACC-100",
                new BigDecimal("100.00"),
                null, userId
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(command)
        );

        verify(accountRepository, never())
                .findAllByAccountNumbersForUpdate(anyList());

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectZeroOrNegativeAmounts() {
        TransferCommand zeroAmount = command(
                "ACC-100", "ACC-200",
                BigDecimal.ZERO, null, userId
        );

        TransferCommand negativeAmount = command(
                "ACC-100", "ACC-200",
                new BigDecimal("-10.00"), null, userId
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(zeroAmount)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(negativeAmount)
        );

        verify(accountRepository, never())
                .findAllByAccountNumbersForUpdate(anyList());
    }

    @Test
    void shouldRejectAmountsWithMoreThanTwoDecimalPlaces() {
        TransferCommand command = command(
                "ACC-100", "ACC-200",
                new BigDecimal("10.001"), null, userId
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(command)
        );

        verify(accountRepository, never())
                .findAllByAccountNumbersForUpdate(anyList());
    }

    private TransferCommand command(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            String idempotencyKey,
            UUID commandUserId
    ) {
        return new TransferCommand(
                fromAccount,
                toAccount,
                amount,
                idempotencyKey,
                commandUserId
        );
    }
}
