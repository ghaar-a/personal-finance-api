
package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.application.service.AccountService;
import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.domain.entity.AccountStatus;
import br.com.tr.personal_finance_api.interfaces.dto.AccountResponse;
import br.com.tr.personal_finance_api.interfaces.mapper.AccountMapper;
import br.com.tr.personal_finance_api.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private AccountController accountController;

    @Test
    void shouldReturnSecureAccountForAuthenticatedUser() {
        UUID accountId = UUID.randomUUID();
        UUID authenticatedUserId = UUID.randomUUID();

        Account account = mock(Account.class);

        AccountResponse expectedResponse = new AccountResponse(
                accountId,
                "ACC-123456",
                new BigDecimal("1500.00"),
                "BRL",
                AccountStatus.ACTIVE
        );

        when(userDetails.getUserId()).thenReturn(authenticatedUserId);

        when(accountService.getAccountSecure(
                accountId,
                authenticatedUserId,
                request
        )).thenReturn(account);

        when(accountMapper.toResponse(account))
                .thenReturn(expectedResponse);

        AccountResponse result = accountController.getAccountSecure(
                accountId,
                userDetails,
                request
        );

        assertSame(expectedResponse, result);

        verify(userDetails).getUserId();

        verify(accountService).getAccountSecure(
                accountId,
                authenticatedUserId,
                request
        );

        verify(accountMapper).toResponse(account);
        verifyNoMoreInteractions(accountService, accountMapper);
    }
}
