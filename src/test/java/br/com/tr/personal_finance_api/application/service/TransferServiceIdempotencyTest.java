
package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.application.command.TransferCommand;
import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.domain.entity.AccountStatus;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.AccountRepository;
import br.com.tr.personal_finance_api.domain.repository.TransactionRepository;
import br.com.tr.personal_finance_api.domain.repository.TransferIdempotencyRepository;
import br.com.tr.personal_finance_api.domain.repository.TransferIdempotencyRepository.ReservationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceIdempotencyTest {

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

    @Test
    @DisplayName("Deve rejeitar reutilização de chave com payload diferente")
    void shouldRejectIdempotencyKeyReusedWithDifferentPayload() {
        when(accountRepository.findAllByAccountNumbersForUpdate(
                List.of("ACC-100", "ACC-200")
        )).thenReturn(List.of(sourceAccount, destinationAccount));

        when(transferIdempotencyRepository.reserveOrValidate(
                userId,
                "transfer-123",
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("250.00"),
                "BRL"
        )).thenReturn(ReservationResult.PAYLOAD_CONFLICT);

        TransferCommand command = new TransferCommand(
                "ACC-100",
                "ACC-200",
                new BigDecimal("250.00"),
                "transfer-123",
                userId
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(command)
        );

        assertEquals(new BigDecimal("1000.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("500.00"), destinationAccount.getBalance());

        verify(transferIdempotencyRepository).reserveOrValidate(
                userId,
                "transfer-123",
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("250.00"),
                "BRL"
        );

        verifyNoInteractions(transactionRepository);
    }
}
