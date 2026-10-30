
package br.com.tr.personal_finance_api.security;

import br.com.tr.personal_finance_api.application.service.AuditLogService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CsrfAttackLoggingFilterTest {

    @Mock
    private AuditLogService auditLogService;

    private CsrfAttackLoggingFilter filter;

    @BeforeEach
    void setup() {
        filter = new CsrfAttackLoggingFilter(auditLogService);
    }

    @Test
    @DisplayName("Modo vulnerável deve registrar POST sem Origin e Referer")
    void vulnerableModeShouldLogPostWithoutOriginAndReferer() throws Exception {
        ReflectionTestUtils.setField(filter, "securityMode", "VULNERABLE");

        MockHttpServletRequest request = createPostRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(auditLogService).logCsrfAttempt(
                "192.0.2.10",
                "/api/csrf/transfer",
                "POST"
        );
    }

    @Test
    @DisplayName("Modo vulnerável não deve registrar POST com Origin")
    void vulnerableModeShouldNotLogPostWithOrigin() throws Exception {
        ReflectionTestUtils.setField(filter, "securityMode", "VULNERABLE");

        MockHttpServletRequest request = createPostRequest();
        request.addHeader("Origin", "https://trusted.example");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(auditLogService, never())
                .logCsrfAttempt(
                        "192.0.2.10",
                        "/api/csrf/transfer",
                        "POST"
                );
    }

    @Test
    @DisplayName("Requisição GET não deve gerar auditoria CSRF")
    void getRequestShouldNotGenerateCsrfAudit() throws Exception {
        ReflectionTestUtils.setField(filter, "securityMode", "VULNERABLE");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/accounts");
        request.setRemoteAddr("192.0.2.10");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(auditLogService, never())
                .logCsrfAttempt(
                        "192.0.2.10",
                        "/api/accounts",
                        "GET"
                );
    }

    @Test
    @DisplayName("Modo seguro não deve registrar tentativas pelo filtro CSRF")
    void secureModeShouldNotLogCsrfAttempt() throws Exception {
        ReflectionTestUtils.setField(filter, "securityMode", "SECURE");

        MockHttpServletRequest request = createPostRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(auditLogService, never())
                .logCsrfAttempt(
                        "192.0.2.10",
                        "/api/csrf/transfer",
                        "POST"
                );
    }

    private MockHttpServletRequest createPostRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/csrf/transfer");
        request.setRemoteAddr("192.0.2.10");

        return request;
    }
}
