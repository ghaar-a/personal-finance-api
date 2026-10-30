package br.com.tr.personal_finance_api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.tr.personal_finance_api.interfaces.exception.ApiError;
import br.com.tr.personal_finance_api.interfaces.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException authException
    ) throws IOException {

        ApiError error = new ApiError(
                ErrorCode.UNAUTHORIZED.name(),
                "Invalid credentials",
                401,
                request.getRequestURI(),
                null
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}