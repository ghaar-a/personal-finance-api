package br.com.tr.personal_finance_api.security;

import br.com.tr.personal_finance_api.application.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CsrfAttackLoggingFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;

    @Value("${app.security-mode:SECURE}")
    private String securityMode;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            if ("VULNERABLE".equalsIgnoreCase(securityMode)) {

                String method = request.getMethod();
                String origin = request.getHeader("Origin");
                String referer = request.getHeader("Referer");

                boolean isStateChanging =
                        "POST".equalsIgnoreCase(method) ||
                                "PUT".equalsIgnoreCase(method) ||
                                "DELETE".equalsIgnoreCase(method);

                boolean missingOrigin = origin == null || origin.isBlank();
                boolean missingReferer = referer == null || referer.isBlank();

                if (isStateChanging && (missingOrigin && missingReferer)) {

                    auditLogService.logCsrfAttempt(
                            request.getRemoteAddr(),
                            request.getRequestURI(),
                            method
                    );
                }
            }

        } catch (Exception ex) {
            // Nunca quebrar a requisição por falha de auditoria
        }

        filterChain.doFilter(request, response);
    }
}