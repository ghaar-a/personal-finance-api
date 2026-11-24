package br.com.tr.personal_finance_api.security;

import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String CSRF_DEMO_PATH = "/api/csrf/transfer";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.security-mode:SECURE}")
    private String securityMode;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UUID userId = jwtService.extractUserId(token);

            User user = userRepository.findById(userId).orElse(null);

            if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            CustomUserDetails userDetails = new CustomUserDetails(user);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null) {
            if (authorizationHeader.startsWith("Bearer ")) {
                return authorizationHeader.substring(7);
            }

            // Um header Authorization presente tem precedência.
            // Não fazemos fallback para cookie se ele estiver malformado.
            return null;
        }

        if (!isCookieAuthenticationAllowed(request)) {
            return null;
        }

        return resolveDemoCookie(request);
    }

    private boolean isCookieAuthenticationAllowed(
            HttpServletRequest request
    ) {
        return "VULNERABLE".equalsIgnoreCase(securityMode)
                && "POST".equalsIgnoreCase(request.getMethod())
                && CSRF_DEMO_PATH.equals(request.getRequestURI());
    }

    private String resolveDemoCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (CsrfDemoCookie.NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}