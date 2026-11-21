package br.com.tr.personal_finance_api.security;

import br.com.tr.personal_finance_api.interfaces.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CustomAuthenticationEntryPointTest {

    private CustomAuthenticationEntryPoint entryPoint;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        entryPoint = new CustomAuthenticationEntryPoint(objectMapper);
    }

    @Test
    void shouldReturnUnauthorizedStatusAndJsonContentType()
            throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/accounts/secure/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new BadCredentialsException("Bad credentials")
        );

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    void shouldWriteExpectedApiErrorFields()
            throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/accounts/secure/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new BadCredentialsException("Bad credentials")
        );

        JsonNode json = objectMapper.readTree(response.getContentAsString());

        assertEquals(ErrorCode.UNAUTHORIZED.name(), json.get("code").asText());
        assertEquals("Invalid credentials", json.get("message").asText());
        assertEquals(401, json.get("status").asInt());
        assertEquals("/api/accounts/secure/123", json.get("path").asText());
        assertTrue(json.get("correlationId").isNull());
        assertTrue(json.hasNonNull("timestamp"));
    }

    @Test
    void shouldUseRequestUriInErrorResponse()
            throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/transfers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new BadCredentialsException("Authentication required")
        );

        JsonNode json = objectMapper.readTree(response.getContentAsString());

        assertEquals("/api/transfers", json.get("path").asText());
    }
}