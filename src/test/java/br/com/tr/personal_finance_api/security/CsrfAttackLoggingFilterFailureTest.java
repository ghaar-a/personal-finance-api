
package br.com.tr.personal_finance_api.security;

import br.com.tr.personal_finance_api.application.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class CsrfAttackLoggingFilterFailureTest {

    private AuditLogService auditLogService;
    private CsrfAttackLoggingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        filter = new CsrfAttackLoggingFilter(auditLogService);
        filterChain = mock(FilterChain.class);

        ReflectionTestUtils.setField(filter, "securityMode", "VULNERABLE");
    }

    @Test
    @DisplayName("Falha na auditoria CSRF não deve interromper a requisição")
    void shouldContinueRequestWhenCsrfAuditFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/csrf/transfer");
        request.setRemoteAddr("192.0.2.10");

        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new IllegalStateException("Audit persistence unavailable"))
                .when(auditLogService)
                .logCsrfAttempt(
                        "192.0.2.10",
                        "/api/csrf/transfer",
                        "POST"
                );

        assertDoesNotThrow(
                () -> filter.doFilterInternal(request, response, filterChain)
        );

        verify(auditLogService).logCsrfAttempt(
                "192.0.2.10",
                "/api/csrf/transfer",
                "POST"
        );

        verify(filterChain).doFilter(
                (HttpServletRequest) request,
                (HttpServletResponse) response
        );
    }
}
