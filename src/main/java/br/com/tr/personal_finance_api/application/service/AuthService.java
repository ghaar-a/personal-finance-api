package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import br.com.tr.personal_finance_api.interfaces.dto.AuthResponse;
import br.com.tr.personal_finance_api.interfaces.dto.LoginRequest;
import br.com.tr.personal_finance_api.interfaces.dto.RegisterRequest;
import br.com.tr.personal_finance_api.interfaces.exception.TooManyRequestsException;
import br.com.tr.personal_finance_api.interfaces.exception.UnauthorizedException;
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
        String password = request.password();
        String ip = getClientIp(httpRequest);

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new UnauthorizedException("Invalid credentials");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            loginAttemptService.recordFailure(email, ip);
            auditLogService.logFailedLogin(email, ip);

            checkBlock(email, ip);

            throw new UnauthorizedException("Invalid credentials");
        }

        boolean passwordMatches =
                passwordEncoder.matches(password, user.getPasswordHash());

        boolean accountEnabled = Boolean.TRUE.equals(user.getEnabled());

        if (!passwordMatches || !accountEnabled) {
            loginAttemptService.recordFailure(email, ip);
            auditLogService.logFailedLogin(email, ip);

            checkBlock(email, ip);

            throw new UnauthorizedException("Invalid credentials");
        }

        loginAttemptService.recordSuccess(email, ip);

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token);
    }

    private void checkBlock(String email, String ip) {
        if (loginAttemptService.isBlocked(email)) {
            auditLogService.logBlockedLogin(email, ip);
            throw new TooManyRequestsException("Too many attempts. Try again later.");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String xfHeader = request.getHeader("X-Forwarded-For");

        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }

        return xfHeader.split(",")[0];
    }
}