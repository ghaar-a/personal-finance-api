
package br.com.tr.personal_finance_api.interfaces.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class SecurityExceptionHandlerTest {

    private ObjectMapper objectMapper;
    private SecurityExceptionHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        handler = new SecurityExceptionHandler(objectMapper);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve retornar JSON 401 com correlation ID do MDC")
    void shouldReturnUnauthorizedResponseWithCorrelationId() throws Exception {
        String correlationId = "request-123";
        MDC.put("correlationId", correlationId);

        MockHttpServletRequest request = request("/api/accounts/secure/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(
                request,
                response,
                new BadCredentialsException("Invalid credentials")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals(ErrorCode.UNAUTHORIZED.name(), body.get("code").asText());
        assertEquals("Unauthorized", body.get("message").asText());
        assertEquals(401, body.get("status").asInt());
        assertEquals("/api/accounts/secure/1", body.get("path").asText());
        assertEquals(correlationId, body.get("correlationId").asText());
        assertTrue(body.hasNonNull("timestamp"));
    }

    @Test
    @DisplayName("Deve usar no-id quando não houver correlation ID no MDC")
    void shouldUseFallbackCorrelationIdWhenMdcIsEmpty() throws Exception {
        MockHttpServletRequest request = request("/api/private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(
                request,
                response,
                new BadCredentialsException("Invalid credentials")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());

        assertEquals(401, response.getStatus());
        assertEquals("no-id", body.get("correlationId").asText());
        assertEquals("/api/private", body.get("path").asText());
    }

    @Test
    @DisplayName("Deve retornar JSON 403 com correlation ID do MDC")
    void shouldReturnForbiddenResponseWithCorrelationId() throws Exception {
        String correlationId = "request-456";
        MDC.put("correlationId", correlationId);

        MockHttpServletRequest request = request("/api/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(
                request,
                response,
                new AccessDeniedException("Insufficient permissions")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());

        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals(ErrorCode.FORBIDDEN.name(), body.get("code").asText());
        assertEquals("Access denied", body.get("message").asText());
        assertEquals(403, body.get("status").asInt());
        assertEquals("/api/admin", body.get("path").asText());
        assertEquals(correlationId, body.get("correlationId").asText());
        assertTrue(body.hasNonNull("timestamp"));
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
