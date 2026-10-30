package br.com.tr.personal_finance_api.interfaces.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    private String getCorrelationId() {
        String id = MDC.get("correlationId");
        return id != null ? id : "no-id";
    }

    // 401 - NÃO AUTENTICADO
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        ApiError error = new ApiError(
                ErrorCode.UNAUTHORIZED.name(),
                "Unauthorized",
                HttpStatus.UNAUTHORIZED.value(),
                request.getRequestURI(),
                getCorrelationId()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        objectMapper.writeValue(response.getOutputStream(), error);
    }

    // 403 - SEM PERMISSÃO
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException ex
    ) throws IOException {

        ApiError error = new ApiError(
                ErrorCode.FORBIDDEN.name(),
                "Access denied",
                HttpStatus.FORBIDDEN.value(),
                request.getRequestURI(),
                getCorrelationId()
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}