package br.com.tr.personal_finance_api.security;

import br.com.tr.personal_finance_api.interfaces.exception.ApiError;
import br.com.tr.personal_finance_api.interfaces.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        String path = request.getRequestURI();

        boolean isLoginEndpoint = path.contains("/auth/login");

        Bucket bucket = rateLimitService.resolveBucket(clientIp, isLoginEndpoint);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 🔒 Rate limit excedido
        ApiError error = new ApiError(
                ErrorCode.TOO_MANY_REQUESTS.name(),
                "Too many requests. Please try again later.",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                path,
                MDC.get("correlationId")
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}