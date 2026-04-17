package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import br.com.tr.personal_finance_api.interfaces.dto.*;
import br.com.tr.personal_finance_api.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;

    public AuthResponse register(RegisterRequest request) {

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {

        String email = request.email();
        String ip = getClientIp(httpRequest);

        if (loginAttemptService.isBlocked(email)) {
            auditLogService.logBlockedLogin(email, ip);
            throw new RuntimeException("Muitas tentativas. Tente novamente mais tarde.");
        }

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {

            loginAttemptService.recordFailure(email, ip);
            auditLogService.logFailedLogin(email, ip);

            throw new RuntimeException("Credenciais inválidas");
        }

        loginAttemptService.recordSuccess(email, ip);

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}