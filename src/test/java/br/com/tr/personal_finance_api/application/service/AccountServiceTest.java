
package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.domain.repository.AccountRepository;
import br.com.tr.personal_finance_api.interfaces.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldFindAccountByIdInVulnerableMode() {
        UUID accountId = UUID.randomUUID();
        Account account = mock(Account.class);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        Account result = accountService.getAccountVulnerable(accountId);

        assertSame(account, result);
        verify(accountRepository).findById(accountId);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldThrowWhenVulnerableAccountDoesNotExist() {
        UUID accountId = UUID.randomUUID();

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.getAccountVulnerable(accountId)
        );

        verify(accountRepository).findById(accountId);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldFindAccountOwnedByAuthenticatedUser() {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Account account = mock(Account.class);

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        Account result = accountService.getAccountSecure(
                accountId,
                userId,
                request
        );

        assertSame(account, result);

        verify(accountRepository).findByIdAndUserId(accountId, userId);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldAuditAndRejectAccountNotOwnedByAuthenticatedUser() {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.getAccountSecure(
                        accountId,
                        userId,
                        request
                )
        );

        verify(accountRepository).findByIdAndUserId(accountId, userId);
        verify(auditLogService).logUnauthorizedAccess(userId, accountId);
    }
}
