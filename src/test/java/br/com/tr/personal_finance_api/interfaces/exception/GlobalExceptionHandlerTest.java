
package br.com.tr.personal_finance_api.interfaces.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldHandleBadCredentials() {
        MDC.put("correlationId", "test-correlation-id");

        ResponseEntity<ApiError> response = handler.handleBadCredentials(
                new BadCredentialsException("Invalid credentials"),
                request
        );

        assertErrorResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "Invalid email or password",
                "/api/test",
                "test-correlation-id"
        );
    }

    @Test
    void shouldHandleUnauthorizedException() {
        ResponseEntity<ApiError> response = handler.handleUnauthorized(
                new UnauthorizedException("Authentication required"),
                request
        );

        assertErrorResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "Authentication required",
                "/api/test",
                "no-id"
        );
    }

    @Test
    void shouldHandleTooManyRequestsException() {
        MDC.put("correlationId", "rate-limit-correlation");

        ResponseEntity<ApiError> response = handler.handleTooManyRequests(
                new TooManyRequestsException("Too many requests"),
                request
        );

        assertErrorResponse(
                response,
                HttpStatus.TOO_MANY_REQUESTS,
                ErrorCode.TOO_MANY_REQUESTS,
                "Too many requests",
                "/api/test",
                "rate-limit-correlation"
        );
    }

    @Test
    void shouldHandleResourceNotFoundException() {
        ResponseEntity<ApiError> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Resource not found"),
                request
        );

        assertErrorResponse(
                response,
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                "Resource not found",
                "/api/test",
                "no-id"
        );
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        ResponseEntity<ApiError> response = handler.handleBadRequest(
                new IllegalArgumentException("Invalid argument"),
                request
        );

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                "Invalid argument",
                "/api/test",
                "no-id"
        );
    }

    @Test
    void shouldHandleAccessDeniedException() {
        ResponseEntity<ApiError> response = handler.handleForbidden(
                new AccessDeniedException("Insufficient permissions"),
                request
        );

        assertErrorResponse(
                response,
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "Access denied",
                "/api/test",
                "no-id"
        );
    }

    @Test
    void shouldHandleGenericExceptionWithMessage() {
        MDC.put("correlationId", "generic-error-correlation");

        ResponseEntity<ApiError> response = handler.handleGenericException(
                new Exception("Unexpected failure"),
                request
        );

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "Unexpected failure",
                "/api/test",
                "generic-error-correlation"
        );
    }

    @Test
    void shouldUseFallbackMessageWhenGenericExceptionMessageIsNull() {
        ResponseEntity<ApiError> response = handler.handleGenericException(
                new Exception((String) null),
                request
        );

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "Unexpected error occurred",
                "/api/test",
                "no-id"
        );
    }

    private void assertErrorResponse(
            ResponseEntity<ApiError> response,
            HttpStatus expectedStatus,
            ErrorCode expectedCode,
            String expectedMessage,
            String expectedPath,
            String expectedCorrelationId
    ) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getStatusCode());

        ApiError body = response.getBody();

        assertNotNull(body);
        assertEquals(expectedCode.name(), body.getCode());
        assertEquals(expectedMessage, body.getMessage());
        assertEquals(expectedStatus.value(), body.getStatus());
        assertEquals(expectedPath, body.getPath());
        assertEquals(expectedCorrelationId, body.getCorrelationId());
        assertNotNull(body.getTimestamp());
    }
}
