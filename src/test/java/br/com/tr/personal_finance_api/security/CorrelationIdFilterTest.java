
package br.com.tr.personal_finance_api.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private static final String HEADER_NAME = "X-Correlation-Id";

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve preservar o correlation ID recebido e disponibilizá-lo no MDC")
    void shouldPreserveProvidedCorrelationId() throws Exception {
        String correlationId = "client-request-123";
        MockHttpServletRequest request = request();
        request.addHeader(HEADER_NAME, correlationId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcValue = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
                mdcValue.set(MDC.get("correlationId"));

        filter.doFilterInternal(request, response, chain);

        assertEquals(correlationId, response.getHeader(HEADER_NAME));
        assertEquals(correlationId, mdcValue.get());
        assertNull(MDC.get("correlationId"));
    }

    @Test
    @DisplayName("Deve gerar UUID quando o header estiver ausente")
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcValue = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
                mdcValue.set(MDC.get("correlationId"));

        filter.doFilterInternal(request, response, chain);

        String responseCorrelationId = response.getHeader(HEADER_NAME);

        assertNotNull(responseCorrelationId);
        assertDoesNotThrow(() -> UUID.fromString(responseCorrelationId));
        assertEquals(responseCorrelationId, mdcValue.get());
        assertNull(MDC.get("correlationId"));
    }

    @Test
    @DisplayName("Deve gerar UUID quando o header estiver em branco")
    void shouldGenerateCorrelationIdWhenHeaderIsBlank() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(HEADER_NAME, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcValue = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
                mdcValue.set(MDC.get("correlationId"));

        filter.doFilterInternal(request, response, chain);

        String responseCorrelationId = response.getHeader(HEADER_NAME);

        assertNotNull(responseCorrelationId);
        assertDoesNotThrow(() -> UUID.fromString(responseCorrelationId));
        assertEquals(responseCorrelationId, mdcValue.get());
        assertNull(MDC.get("correlationId"));
    }

    @Test
    @DisplayName("Deve limpar o MDC mesmo quando a cadeia de filtros lançar exceção")
    void shouldClearMdcWhenFilterChainThrowsException() {
        MockHttpServletRequest request = request();
        request.addHeader(HEADER_NAME, "request-with-error");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (servletRequest, servletResponse) -> {
            assertEquals("request-with-error", MDC.get("correlationId"));
            throw new IOException("Filter chain failure");
        };

        assertThrows(
                IOException.class,
                () -> filter.doFilterInternal(request, response, chain)
        );

        assertNull(MDC.get("correlationId"));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        return request;
    }
}
