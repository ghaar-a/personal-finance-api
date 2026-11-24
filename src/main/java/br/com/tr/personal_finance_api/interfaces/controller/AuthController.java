package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.application.service.AuthService;
import br.com.tr.personal_finance_api.interfaces.dto.AuthResponse;
import br.com.tr.personal_finance_api.interfaces.dto.LoginRequest;
import br.com.tr.personal_finance_api.interfaces.dto.RegisterRequest;
import br.com.tr.personal_finance_api.security.CsrfDemoCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.security-mode:SECURE}")
    private String securityMode;

    @Value("${spring.security.jwt.expiration:3600000}")
    private long jwtExpiration;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request
    ) {
        AuthResponse authResponse = authService.register(request);

        return ResponseEntity.ok()
                .headers(createAuthenticationHeaders(authResponse))
                .body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        AuthResponse authResponse = authService.login(
                request,
                httpServletRequest
        );

        return ResponseEntity.ok()
                .headers(createAuthenticationHeaders(authResponse))
                .body(authResponse);
    }

    private HttpHeaders createAuthenticationHeaders(
            AuthResponse authResponse
    ) {
        HttpHeaders headers = new HttpHeaders();

        if ("VULNERABLE".equalsIgnoreCase(securityMode)) {
            ResponseCookie cookie = ResponseCookie
                    .from(CsrfDemoCookie.NAME, authResponse.token())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path(CsrfDemoCookie.PATH)
                    .maxAge(Duration.ofMillis(jwtExpiration))
                    .build();

            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return headers;
    }
}