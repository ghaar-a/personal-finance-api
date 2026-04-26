package br.com.tr.personal_finance_api.interfaces.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private String getCorrelationId() {
        return MDC.get("correlationId");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {

        ApiError error = new ApiError(
                ErrorCode.INTERNAL_ERROR.name(),
                "Unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI(),
                getCorrelationId()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        ApiError error = new ApiError(
                ErrorCode.BAD_REQUEST.name(),
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                getCorrelationId()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(
            Exception ex,
            HttpServletRequest request
    ) {

        ApiError error = new ApiError(
                ErrorCode.FORBIDDEN.name(),
                "Access denied",
                HttpStatus.FORBIDDEN.value(),
                request.getRequestURI(),
                getCorrelationId()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            Exception ex,
            HttpServletRequest request
    ) {

        ApiError error = new ApiError(
                ErrorCode.UNAUTHORIZED.name(),
                "Authentication required",
                HttpStatus.UNAUTHORIZED.value(),
                request.getRequestURI(),
                getCorrelationId()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}