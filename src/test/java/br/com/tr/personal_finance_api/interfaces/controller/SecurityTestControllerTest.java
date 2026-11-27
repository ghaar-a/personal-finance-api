
package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.security.mode.SecureAccountService;
import br.com.tr.personal_finance_api.security.mode.SecurityMode;
import br.com.tr.personal_finance_api.security.mode.VulnerableAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityTestControllerTest {

    @Mock
    private VulnerableAccountService vulnerableService;

    @Mock
    private SecureAccountService secureService;

    @InjectMocks
    private SecurityTestController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                controller,
                "mode",
                SecurityMode.SECURE
        );
    }

    @Test
    void shouldUseVulnerableServiceWhenSecurityModeIsVulnerable() {
        String accountNumber = "ACC-001";

        List<Map<String, Object>> expectedResult = List.of(
                Map.of(
                        "account_number", accountNumber,
                        "balance", 1000
                )
        );

        ReflectionTestUtils.setField(
                controller,
                "mode",
                SecurityMode.VULNERABLE
        );

        when(vulnerableService.findAccountByNumber(accountNumber))
                .thenReturn(expectedResult);

        List<Map<String, Object>> result =
                controller.test(accountNumber);

        assertSame(expectedResult, result);

        verify(vulnerableService).findAccountByNumber(accountNumber);
        verifyNoInteractions(secureService);
    }

    @Test
    void shouldUseSecureServiceWhenSecurityModeIsSecure() {
        String accountNumber = "ACC-002";

        List<Map<String, Object>> expectedResult = List.of(
                Map.of(
                        "account_number", accountNumber,
                        "balance", 2000
                )
        );

        when(secureService.findAccountByNumber(accountNumber))
                .thenReturn(expectedResult);

        List<Map<String, Object>> result =
                controller.test(accountNumber);

        assertSame(expectedResult, result);

        verify(secureService).findAccountByNumber(accountNumber);
        verifyNoInteractions(vulnerableService);
    }
}
